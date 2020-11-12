package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;

import static assertion.Assert.assertNotNull;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean changed = false;
    private final Body body;
    private final SootMethod method;
    private final ISourceSinkManager sourceSinkManager;
    private final ITaintWrapper taintWrapper;
    private final Taint entryTaint;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<Taint, List<Set<Taint>>> currMethodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;
    private final Map<Taint, Taint> currTaintCache;
    private final List<Taint> sources;
    private final Set<String> basicParamTypeSet;
    private final PhantomRetStmt phantomRetStmt;

    public TaintFlowAnalysis(Body body, ISourceSinkManager sourceSinkManager) {
        this(body, sourceSinkManager, Taint.getEmptyTaint(), new HashMap<>(), new HashMap<>(), null);
    }

    public TaintFlowAnalysis(Body body,
                             ISourceSinkManager sourceSinkManager,
                             Taint entryTaint,
                             Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary,
                             Map<SootMethod, Map<Taint, Taint>> methodTaintCache,
                             ITaintWrapper taintWrapper) {
        super(new ExceptionalUnitGraph(body));
        this.body = body;
        this.method = body.getMethod();
        this.sourceSinkManager = sourceSinkManager;
        this.entryTaint = entryTaint;
        this.methodSummary = methodSummary;
        this.methodTaintCache = methodTaintCache;
        this.sources = new ArrayList<>();
        this.taintWrapper = taintWrapper;
        this.phantomRetStmt = PhantomRetStmt.getInstance(method);

        // Sanity check
        assertNotNull(body);
        assertNotNull(sourceSinkManager);
        assertNotNull(entryTaint);
        assertNotNull(methodSummary);
        assertNotNull(methodTaintCache);

        // Initialize methodSummary and methodTaintCache for current method (if not done yet)
        methodSummary.putIfAbsent(method, new HashMap<>());
        this.currMethodSummary = methodSummary.get(method);
        methodTaintCache.putIfAbsent(method, new HashMap<>());
        this.currTaintCache = methodTaintCache.get(method);

        // Initialize the taint summary for current method with the input entry taint (if not done yet)
        // Summary list format: idx 0: (set of taints on) base, 1: retVal, 2+: parameters
        if (this.currMethodSummary.get(entryTaint) == null) {
            List<Set<Taint>> summary = new ArrayList<>();
            for (int i = 0; i < method.getParameterCount() + 2; i++) {
                summary.add(new HashSet<>());
            }
            this.currMethodSummary.put(entryTaint, summary);
        }

        // initialize basic param type set
        this.basicParamTypeSet = new HashSet<>(Arrays.asList(
                "int",
                "float",
                "long",
                "boolean"
        ));
    }

    public boolean isChanged() {
        return changed;
    }

    public List<Taint> getSources() {
        return sources;
    }

    public void doAnalysis() {
        logger.debug("Analyzing method {}", method);
        super.doAnalysis();
    }

    @Override
    protected void flowThrough(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.clear();
        out.addAll(in);

        Stmt stmt = (Stmt) unit;

        if (stmt instanceof AssignStmt) {
            visitAssign(in, (AssignStmt) stmt, out);
        }

        if (stmt instanceof InvokeStmt) {
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (!sourceSinkManager.isSource(stmt)) {
                visitInvoke(in, stmt, invoke, out);
            }
        }

        if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
            visitReturn(in, stmt);
        }

        if (stmt instanceof IfStmt) {
            visitIf(in, (IfStmt) stmt, out);
        }

        if (sourceSinkManager.isSink(stmt)) {
            visitSink(in, stmt);
        }
    }

    private void visitAssign(Set<Taint> in, AssignStmt stmt, Set<Taint> out) {
        Value leftOp = stmt.getLeftOp();
        Value rightOp = stmt.getRightOp();

        // KILL
        for (Taint t : in) {
            if (t.associatesWith(leftOp)) {
                out.remove(t);
            }
        }

        // GEN
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (sourceSinkManager.isSource(stmt)) {
                Taint newTaint = Taint.getTaintFor(leftOp, stmt, method, currTaintCache);
                if (!sources.contains(newTaint)) {
                    sources.add(newTaint);
                }
                out.add(newTaint);
            } else {
                visitInvoke(in, stmt, invoke, out);
            }
        } else {
            for (Taint t : in) {
                for (ValueBox box : rightOp.getUseBoxes()) {
                    Value value = box.getValue();
                    if (t.taints(value)) {
                        Taint newTaint = Taint.getTaintFor(leftOp, stmt, method, currTaintCache);
                        t.addSuccessor(newTaint);
                        out.add(newTaint);
                    }
                }
            }
        }
    }

    private void visitInvoke(Set<Taint> in, Stmt stmt, InvokeExpr invoke, Set<Taint> out) {
        SootMethod callee = invoke.getMethod();
        assertNotNull(callee);

        // Check if taint wrapper applies
        if (taintWrapper != null && taintWrapper.supportsCallee(callee)) {
            taintWrapper.genTaintsForMethodInternal(in, stmt, method, out, currTaintCache);
            return;
        }

        if (!callee.hasActiveBody()) {
            logger.debug("No active body for callee {} in {}", callee, method);
            return;
        }
        Body calleeBody = callee.getActiveBody();

        // Get the base object of this invocation in caller and the corresponding this object in callee (if exists)
        Value base = null;
        Value calleeThisLocal = null;
        if (invoke instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invoke).getBase();
            calleeThisLocal = calleeBody.getThisLocal();
        }

        // Get the retVal of this invocation in caller (if applies)
        Value retVal = null;
        if (stmt instanceof AssignStmt) {
            retVal = ((AssignStmt) stmt).getLeftOp();
        }

        // Initialize methodSummary and methodTaintCache for callee (if not done yet)
        methodSummary.putIfAbsent(callee, new HashMap<>());
        Map<Taint, List<Set<Taint>>> calleeSummary = methodSummary.get(callee);
        methodTaintCache.putIfAbsent(callee, new HashMap<>());
        Map<Taint, Taint> calleeTaintCache = methodTaintCache.get(callee);

        // Initialize the empty taint summary for callee (if not done yet)
        // Summary list format: idx 0: (set of taints on) base, 1: retVal, 2+: parameters
        if (calleeSummary.get(Taint.getEmptyTaint()) == null) {
            List<Set<Taint>> emptyTaintSummary = new ArrayList<>();
            for (int i = 0; i < callee.getParameterCount() + 2; i++) {
                emptyTaintSummary.add(new HashSet<>());
            }
            calleeSummary.put(Taint.getEmptyTaint(), emptyTaintSummary);
        }

        // Initialize the summary for this invocation by elements copied from the empty taint summary
        List<Set<Taint>> summary = new ArrayList<>();
        for (Set<Taint> taints : calleeSummary.get(Taint.getEmptyTaint())) {
            Set<Taint> newTaints = new HashSet<>();
            newTaints.addAll(taints);
            summary.add(newTaints);
        }

        // Gather summary info for this invocation
        for (Taint t : in) {
            // Process base object
            if (base != null && t.associatesWith(base)) {
                out.remove(t);
                genCalleeEntryTaints(t, calleeThisLocal, stmt, calleeSummary, calleeTaintCache, summary);
            }

            // Process parameters
            for (int i = 0; i < invoke.getArgCount(); i++) {
                Value arg = invoke.getArg(i);
                if (t.associatesWith(arg)) {
                    // Check if the param is basic type (we should pass on the taint in that case)
                    String paramType = arg.getType().toString();
                    if (!basicParamTypeSet.contains(paramType)) {
                        out.remove(t);
                    }

                    Local calleeParam = calleeBody.getParameterLocal(i);
                    genCalleeEntryTaints(t, calleeParam, stmt, calleeSummary, calleeTaintCache, summary);
                }
            }
        }

        // Process base object
        if (base != null) {
            Set<Taint> baseTaints = summary.get(0);
            genTaintsFromInvokeSummary(baseTaints, base, stmt, out);
        }

        // Process return value
        if (retVal != null) {
            Set<Taint> retTaints = summary.get(1);
            genTaintsFromInvokeSummary(retTaints, retVal, stmt, out);
        }

        // Process parameters
        for (int i = 0; i < invoke.getArgCount(); i++) {
            Value arg = invoke.getArg(i);
            Set<Taint> argTaints = summary.get(2 + i);
            genTaintsFromInvokeSummary(argTaints, arg, stmt, out);
        }
    }

    private void genCalleeEntryTaints(Taint callerTaint, Value calleeVal, Stmt stmt,
                                      Map<Taint, List<Set<Taint>>> calleeSummary,
                                      Map<Taint, Taint> calleeTaintCache,
                                      List<Set<Taint>> summary) {
        // Send caller taint to callee
        Taint calleeTaint = Taint.getTransferredTaintFor(
                callerTaint, calleeVal, stmt, method, calleeTaintCache, Taint.TransferType.Call);
        callerTaint.addSuccessor(calleeTaint);

        // Receive callee taint summary for the sent caller taint
        List<Set<Taint>> lst = calleeSummary.get(calleeTaint);
        if (lst != null) {
            for (int i = 0; i < lst.size(); i++) {
                summary.get(i).addAll(lst.get(i));
            }
        } else {
            calleeSummary.put(calleeTaint, null);
        }
    }

    private void genTaintsFromInvokeSummary(Set<Taint> taints, Value callerVal, Stmt stmt, Set<Taint> out) {
        for (Taint t : taints) {
            Taint callerTaint = Taint.getTransferredTaintFor(
                    t, callerVal, stmt, method, currTaintCache, Taint.TransferType.Return);
            t.addSuccessor(callerTaint);
            out.add(callerTaint);
        }
    }

    private void visitReturn(Set<Taint> in, Stmt stmt) {
        // Get the local representing @this (if exists)
        Local thiz = null;
        if (!body.getMethod().isStatic()) {
            thiz = body.getThisLocal();
        }

        // Get return value (if exists)
        Value retVal = null;
        if (stmt instanceof ReturnStmt) {
            retVal = ((ReturnStmt) stmt).getOp();
        }

        // Get the list of Locals representing the parameters (on LHS of IdentityStmt)
        List<Local> paramLocals = body.getParameterLocals();

        List<Set<Taint>> summary = currMethodSummary.get(entryTaint);
        for (Taint t : in) {
            // Check if t taints base object
            if (thiz != null && t.associatesWith(thiz)) {
                Taint newTaint = Taint.getTaintFor(t.getValue(), phantomRetStmt, method, currTaintCache);
                t.addSuccessor(newTaint);
                changed |= summary.get(0).add(newTaint);
            }

            // Check if t taints return value
            if (retVal != null && t.associatesWith(retVal)) {
                Taint newTaint = Taint.getTaintFor(t.getValue(), phantomRetStmt, method, currTaintCache);
                t.addSuccessor(newTaint);
                changed |= summary.get(1).add(newTaint);
            }

            // Check if t taints object-type parameters
            for (int i = 0; i < paramLocals.size(); i++) {
                Local paramLocal = paramLocals.get(i);
                // Check if the param is basic type (we should not taint them in that case)
                String paramType = paramLocal.getType().toString();
                if (!basicParamTypeSet.contains(paramType) && t.associatesWith(paramLocal)) {
                    Taint newTaint = Taint.getTaintFor(t.getValue(), phantomRetStmt, method, currTaintCache);
                    t.addSuccessor(newTaint);
                    changed |= summary.get(2 + i).add(newTaint);
                }
            }
        }
    }

    private void visitIf(Set<Taint> in, IfStmt stmt, Set<Taint> out) {
        for (Taint t : in) {
            for (ValueBox box : stmt.getUseBoxes()) {
                Value value = box.getValue();
                if (t.taints(value)) {
                    out.remove(t);
                    Taint newTaint = Taint.getTaintFor(t.getValue(), stmt, method, currTaintCache);
                    t.addSuccessor(newTaint);
                    out.add(newTaint);
                }
            }
        }
    }

    private void visitSink(Set<Taint> in, Stmt stmt) {
        for (Taint t : in) {
            for (ValueBox box : stmt.getUseBoxes()) {
                Value value = box.getValue();
                if (t.taints(value)) {
                    Taint newTaint = Taint.getTaintFor(t.getValue(), stmt, method, currTaintCache);
                    t.addSuccessor(newTaint);
                }
            }
        }
    }

    @Override
    protected Set<Taint> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected Set<Taint> entryInitialFlow() {
        Set<Taint> entryTaints = new HashSet<>();
        if (!entryTaint.isEmpty()) {
            entryTaints.add(entryTaint);
        }
        return entryTaints;
    }

    @Override
    protected void merge(Set<Taint> in1, Set<Taint> in2, Set<Taint> out) {
        out.clear();
        out.addAll(in1);
        out.addAll(in2);
    }

    @Override
    protected void copy(Set<Taint> source, Set<Taint> dest) {
        dest.clear();
        dest.addAll(source);
    }

}
