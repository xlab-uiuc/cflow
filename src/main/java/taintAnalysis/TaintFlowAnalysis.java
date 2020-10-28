package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

import static assertion.Assert.assertNotNull;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean changed = false;
    private final Body body;
    private final SootMethod method;
    private final ConfigInterface configInterface;
    private final Taint entryTaint;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<Taint, List<Set<Taint>>> currMethodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;
    private final Map<Taint, Taint> currTaintCache;
    private final List<Taint> sources;
    private final Set<String> basicParamTypeSet;

    public TaintFlowAnalysis(Body body, ConfigInterface configInterface) {
        this(body, configInterface, Taint.getEmptyTaint(), new HashMap<>(), new HashMap<>());
    }

    public TaintFlowAnalysis(Body body,
                             ConfigInterface configInterface,
                             Taint entryTaint,
                             Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary,
                             Map<SootMethod, Map<Taint, Taint>> methodTaintCache) {
        super(new ExceptionalUnitGraph(body));
        this.body = body;
        this.method = body.getMethod();
        this.configInterface = configInterface;
        this.entryTaint = entryTaint;
        this.methodSummary = methodSummary;
        this.methodTaintCache = methodTaintCache;
        this.sources = new ArrayList<>();

        // Sanity check
        assertNotNull(body);
        assertNotNull(configInterface);
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
        this.basicParamTypeSet = new HashSet<String>(Arrays.asList(
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
//        logger.info("Analyzing method {}", body.getMethod());
        System.out.println("##### do analysis on " + body.getMethod().toString());
        super.doAnalysis();
    }

    @Override
    protected void flowThrough(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.clear();
        out.addAll(in);

        if (unit instanceof AssignStmt) {
            visitAssign(in, (AssignStmt) unit, out);
        }

        if (unit instanceof InvokeStmt) {
            InvokeStmt stmt = (InvokeStmt) unit;
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (!configInterface.isGetter(invoke)) {
                visitInvoke(in, stmt, invoke, null, out);
            }
        }

        if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
            visitReturn(in, (Stmt) unit, out);
        }
    }

    private void visitAssign(Set<Taint> in, AssignStmt stmt, Set<Taint> out) {
        Value leftOp = stmt.getLeftOp();

        // KILL
        for (Taint t : in) {
            if (t.taints(leftOp)) {
                out.remove(t);
            }
        }

        // GEN
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (configInterface.isGetter(invoke)) {
                Taint newTaint = new Taint(leftOp, stmt, method);
                if (currTaintCache.containsKey(newTaint)) {
                    newTaint = currTaintCache.get(newTaint);
                } else {
                    changed = true;
                    sources.add(newTaint);
                    currTaintCache.put(newTaint, newTaint);
                }
                out.add(newTaint);
            } else {
                visitInvoke(in, stmt, stmt.getInvokeExpr(), leftOp, out);
            }
        } else {
            for (Taint t : in) {
                for (ValueBox box : stmt.getUseBoxes()) {
                    Value value = box.getValue();
                    if (t.taints(value)) {
                        Taint newTaint = new Taint(leftOp, stmt, method);
                        if (currTaintCache.containsKey(newTaint)) {
                            newTaint = currTaintCache.get(newTaint);
                        } else {
                            changed = true;
                            currTaintCache.put(newTaint, newTaint);
                        }
                        t.addSuccessor(newTaint);
                        out.add(newTaint);
                    }
                }
            }
        }
    }

    private void visitInvoke(Set<Taint> in, Stmt stmt, InvokeExpr invoke, Value retVal, Set<Taint> out) {
        SootMethod callee = invoke.getMethod();
        logger.info("visit invoke {}", invoke);
        // Sanity check
        assertNotNull(callee);
        if (!callee.hasActiveBody()) {
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
        Taint calleeTaint = callerTaint.transferTaintTo(calleeVal, stmt, method);
        if (calleeTaintCache.containsKey(calleeTaint)) {
            calleeTaint = calleeTaintCache.get(calleeTaint);
        } else {
            changed = true;
            calleeTaintCache.put(calleeTaint, calleeTaint);
        }
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
            Taint callerTaint = t.transferTaintTo(callerVal, stmt, method);
            if (currTaintCache.containsKey(callerTaint)) {
                callerTaint = currTaintCache.get(callerTaint);
            } else {
                changed = true;
                currTaintCache.put(callerTaint, callerTaint);
            }
            t.addSuccessor(callerTaint);
            out.add(callerTaint);
        }
    }

    private void visitReturn(Set<Taint> in, Stmt stmt, Set<Taint> out) {
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
                summary.get(0).add(t);
            }

            // Check if t taints return value
            if (retVal != null && t.associatesWith(retVal)) {
                summary.get(1).add(t);
            }

            // Check if t taints object-type parameters
            for (int i = 0; i < paramLocals.size(); i++) {
                Local paramLocal = paramLocals.get(i);
                // Check if the param is basic type (we should not taint them in that case)
                String paramType = paramLocal.getType().toString();
                if (!basicParamTypeSet.contains(paramType) && t.associatesWith(paramLocal)) {
                    summary.get(2 + i).add(t);
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
