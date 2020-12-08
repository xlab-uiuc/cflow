package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;
import taintAnalysis.utility.PhantomIdentityStmt;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;

import static assertion.Assert.assertNotNull;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final CallGraph cg = Scene.v().hasCallGraph() ? Scene.v().getCallGraph() : null;

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
        if (!this.currMethodSummary.containsKey(entryTaint)) {
            changed = true;
            List<Set<Taint>> summary = new ArrayList<>();
            for (int i = 0; i < method.getParameterCount() + 2; i++) {
                summary.add(new HashSet<>());
            }
            this.currMethodSummary.put(entryTaint, summary);
        }
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

//        if (stmt instanceof IfStmt) {
//            visitIf(in, (IfStmt) stmt, out);
//        }
//
//        if (sourceSinkManager.isSink(stmt)) {
//            visitSink(in, stmt);
//        }
    }

    private void visitAssign(Set<Taint> in, AssignStmt stmt, Set<Taint> out) {
        Value leftOp = stmt.getLeftOp();
        Value rightOp = stmt.getRightOp();

        // KILL
        for (Taint t : in) {
            if (t.taints(leftOp)) {
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
                if (t.taints(rightOp)) {
                    Taint newTaint;
                    if (leftOp.getType() instanceof PrimType || rightOp instanceof InstanceFieldRef) {
                        newTaint = Taint.getTaintFor(leftOp, stmt, method, currTaintCache);
                    } else {
                        newTaint = Taint.getTransferredTaintFor(
                                t, leftOp, stmt, method, currTaintCache, Taint.TransferType.None);
                    }
                    t.addSuccessor(newTaint);
                    out.add(newTaint);
                }
            }
        }
    }

    private void visitInvoke(Set<Taint> in, Stmt stmt, InvokeExpr invoke, Set<Taint> out) {
        SootMethod calleeMethod = invoke.getMethod();
        assertNotNull(calleeMethod);

        // Check if taint wrapper applies
        if (taintWrapper != null && taintWrapper.supportsCallee(calleeMethod)) {
            Set<Taint> killSet = new HashSet<>();
            Set<Taint> genSet = new HashSet<>();
            taintWrapper.genTaintsForMethodInternal(in, stmt, method, killSet, genSet, currTaintCache);
            for (Taint t : killSet) {
                out.remove(t);
            }
            for (Taint t : genSet) {
                out.add(t);
            }
            return;
        }

        // Get all possible callees for this call site
        List<SootMethod> methods = new ArrayList<>();
        if (cg == null) {
            methods.add(calleeMethod);
        } else {
            for (Iterator<Edge> it = cg.edgesOutOf(stmt); it.hasNext(); ) {
                Edge edge = it.next();
                SootMethod sm = edge.tgt();
                if (calleeMethod.getName().equals(sm.getName())) {
                    methods.add(sm);
                }
            }
        }

        // Get the base object of this invocation in caller (if applies)
        Value base = null;
        if (invoke instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invoke).getBase();
        }

        // Get the retVal of this invocation in caller (if applies)
        Value retVal = null;
        if (stmt instanceof AssignStmt) {
            retVal = ((AssignStmt) stmt).getLeftOp();
        }

        // Compute KILL and GEN
        List<Set<Taint>> killSets = new ArrayList<>();
        List<Set<Taint>> genSets = new ArrayList<>();
        for (SootMethod callee : methods) {
            if (!callee.hasActiveBody()) {
                logger.debug("No active body for callee {} in {}", callee, method);
                continue;
            }
            Body calleeBody = callee.getActiveBody();

            Set<Taint> killSet = new HashSet<>();
            Set<Taint> genSet = new HashSet<>();
            killSets.add(killSet);
            genSets.add(genSet);

            // Get this object in callee (if exists)
            Value calleeThisLocal = null;
            if (invoke instanceof InstanceInvokeExpr) {
                calleeThisLocal = calleeBody.getThisLocal();
            }

            // Initialize methodSummary and methodTaintCache for callee (if not done yet)
            methodSummary.putIfAbsent(callee, new HashMap<>());
            Map<Taint, List<Set<Taint>>> calleeSummary = methodSummary.get(callee);
            methodTaintCache.putIfAbsent(callee, new HashMap<>());
            Map<Taint, Taint> calleeTaintCache = methodTaintCache.get(callee);

            // Initialize the empty taint summary for callee (if not done yet)
            // Summary list format: idx 0: (set of taints on) base, 1: retVal, 2+: parameters
            if (!calleeSummary.containsKey(Taint.getEmptyTaint())) {
                changed = true;
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

            // Compute KILL and gather summary info for this invocation
            for (Taint t : in) {
                // Process base object
                if (base != null && t.taints(base)) {
                    killSet.add(t);
                    genCalleeEntryTaints(t, calleeThisLocal, stmt, calleeSummary, calleeTaintCache, summary, callee);
                }

                // Process parameters
                for (int i = 0; i < invoke.getArgCount(); i++) {
                    Value arg = invoke.getArg(i);
                    if (t.taints(arg)) {
                        // Check if the param is basic type (we should pass on the taint in that case)
                        if (!(arg.getType() instanceof PrimType)) {
                            killSet.add(t);
                        }
                        Local calleeParam = calleeBody.getParameterLocal(i);
                        genCalleeEntryTaints(t, calleeParam, stmt, calleeSummary, calleeTaintCache, summary, callee);
                    }
                }
            }

            // Compute GEN from the gathered summary info
            // Process base object
            if (base != null) {
                Set<Taint> baseTaints = summary.get(0);
                genSet.addAll(getTaintsFromInvokeSummary(baseTaints, base, stmt));
            }

            // Process return value
            if (retVal != null) {
                Set<Taint> retTaints = summary.get(1);
                genSet.addAll(getTaintsFromInvokeSummary(retTaints, retVal, stmt));
            }

            // Process parameters
            for (int i = 0; i < invoke.getArgCount(); i++) {
                Value arg = invoke.getArg(i);
                Set<Taint> argTaints = summary.get(2 + i);
                genSet.addAll(getTaintsFromInvokeSummary(argTaints, arg, stmt));
            }
        }

        // KILL the INTERSECTION of all kill sets
        Set<Taint> killSet = new HashSet<>();
        for (int i = 0; i < killSets.size(); i++) {
            if (i == 0) {
                killSet.addAll(killSets.get(0));
            } else {
                killSet.retainAll(killSets.get(i));
            }
        }
        for (Taint t : killSet) {
            out.remove(t);
        }

        // GEN the UNION of all gen sets
        Set<Taint> genSet = new HashSet<>();
        for (Set<Taint> s : genSets) {
            genSet.addAll(s);
        }
        for (Taint t : genSet) {
            out.add(t);
        }
    }

    private void genCalleeEntryTaints(Taint t, Value calleeVal, Stmt stmt,
                                      Map<Taint, List<Set<Taint>>> calleeSummary,
                                      Map<Taint, Taint> calleeTaintCache,
                                      List<Set<Taint>> summary,
                                      SootMethod callee) {
         // Generate caller taint at call site
         Taint callerTaint = Taint.getTransferredTaintFor(
                 t, t.getPlainValue(), stmt, method, currTaintCache, Taint.TransferType.Call);
         t.addSuccessor(callerTaint);

        // Send caller taint to callee
        PhantomIdentityStmt phantomIdentityStmt = PhantomIdentityStmt.getInstance(callee);
        Taint calleeTaint = Taint.getTransferredTaintFor(
                callerTaint, calleeVal, phantomIdentityStmt, callee, calleeTaintCache);
        callerTaint.addSuccessor(calleeTaint);

        // Receive callee taint summary for the sent caller taint
        if (calleeSummary.containsKey(calleeTaint)) {
            List<Set<Taint>> lst = calleeSummary.get(calleeTaint);
            for (int i = 0; i < lst.size(); i++) {
                summary.get(i).addAll(lst.get(i));
            }
        } else {
            // Generate new summary entry for the callee taint
            changed = true;
            List<Set<Taint>> newSummary = new ArrayList<>();
            for (int i = 0; i < callee.getParameterCount() + 2; i++) {
                newSummary.add(new HashSet<>());
            }
            calleeSummary.put(calleeTaint, newSummary);
        }
    }

    private Set<Taint> getTaintsFromInvokeSummary(Set<Taint> taints, Value callerVal, Stmt stmt) {
        Set<Taint> out = new HashSet<>();
        if (callerVal instanceof NullConstant) {
            return out;
        }
        for (Taint t : taints) {
            Taint callerTaint = Taint.getTransferredTaintFor(
                    t, callerVal, stmt, method, currTaintCache, Taint.TransferType.Return);
            t.addSuccessor(callerTaint);
            out.add(callerTaint);
        }
        return out;
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
            if (thiz != null && t.taints(thiz)) {
                Taint newTaint = Taint.getTransferredTaintFor(
                        t, t.getPlainValue(), phantomRetStmt, method, currTaintCache);
                t.addSuccessor(newTaint);
                changed |= summary.get(0).add(newTaint);
            }

            // Check if t taints return value
            if (retVal != null && t.taints(retVal)) {
                Taint newTaint = Taint.getTransferredTaintFor(
                        t, t.getPlainValue(), phantomRetStmt, method, currTaintCache);
                t.addSuccessor(newTaint);
                changed |= summary.get(1).add(newTaint);
            }

            // Check if t taints object-type parameters
            for (int i = 0; i < paramLocals.size(); i++) {
                Local paramLocal = paramLocals.get(i);
                // Check if the param is basic type (we should not taint them in that case)
                if (!(paramLocal.getType() instanceof PrimType) && t.taints(paramLocal)) {
                    Taint newTaint = Taint.getTransferredTaintFor(
                            t, t.getPlainValue(), phantomRetStmt, method, currTaintCache);
                    t.addSuccessor(newTaint);
                    changed |= summary.get(2 + i).add(newTaint);
                }
            }
        }
    }

    private void visitIf(Set<Taint> in, IfStmt stmt, Set<Taint> out) {
        Value condition = stmt.getCondition();
        for (Taint t : in) {
            if (t.taints(condition)) {
                out.remove(t);
                Taint newTaint = Taint.getTransferredTaintFor(
                        t, t.getPlainValue(), stmt, method, currTaintCache);
                t.addSuccessor(newTaint);
                out.add(newTaint);
            }
        }
    }

    private void visitSink(Set<Taint> in, Stmt stmt) {
//        List<ValueBox> valueBoxes = new ArrayList<>();
//        if (stmt instanceof DefinitionStmt) {
//            DefinitionStmt definitionStmt = (DefinitionStmt) stmt;
//            valueBoxes.add(definitionStmt.getRightOpBox());
//            valueBoxes.addAll(definitionStmt.getRightOp().getUseBoxes());
//        } else {
//            valueBoxes.addAll(stmt.getUseBoxes());
//        }
//        for (Taint t : in) {
//            for (ValueBox box : valueBoxes) {
//                Value value = box.getValue();
//                if (t.taints(value)) {
//                    Taint newTaint = Taint.getTransferredTaintFor(
//                            t, t.getValue(), stmt, method, currTaintCache);
//                    t.addSuccessor(newTaint);
//                }
//            }
//        }
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
