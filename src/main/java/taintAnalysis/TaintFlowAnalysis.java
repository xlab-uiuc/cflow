package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean changed = false;
    private final Body body;
    private final SootMethod method;
    private final ConfigInterface configInterface;
    private final Set<Taint> entryInitialFlow;
    private final Map<SootMethod, Map<Taint, Set<Taint>>> methodSummary;
    private Map<Stmt, Taint> stmtTaintCache;
    private final List<Taint> sources;
    private final List<Unit> exitUnits;

    public TaintFlowAnalysis(Body body, ConfigInterface configInterface) {
        this(body, configInterface, new HashSet<>(), new HashMap<>(), new HashMap<>());
    }

    public TaintFlowAnalysis(Body body,
                             ConfigInterface configInterface,
                             Set<Taint> entryInitialFlow,
                             Map<SootMethod, Map<Taint, Set<Taint>>> methodSummary,
                             Map<Stmt, Taint> stmtTaintCache) {
        super(new ExceptionalUnitGraph(body));
        this.body = body;
        this.method = body.getMethod();
        this.configInterface = configInterface;
        this.entryInitialFlow = entryInitialFlow;
        this.methodSummary = methodSummary;
        this.stmtTaintCache = stmtTaintCache;
        this.sources = new ArrayList<>();
        this.exitUnits = new ArrayList<>();

        // Only collect return statements as exitUnits, as we don't do exceptional taint tracking
        for (Unit unit : graph.getTails()) {
            if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
                this.exitUnits.add(unit);
            }
        }

//        logger.info("Analyzing method {}", body.getMethod());
//        System.out.println(body.getMethod());
    }

    public boolean isChanged() {
        return changed;
    }

    public List<Taint> getSources() {
        return sources;
    }

    public void doAnalysis() {
        super.doAnalysis();
        doPostAnalysis();
    }

    private void doPostAnalysis() {
        // Compute exit flow as union of outsets of all return statements
        Set<Taint> exitFlow = new HashSet<>();
        for (Unit unit : exitUnits) {
            exitFlow.addAll(getFlowAfter(unit));
        }

        // TODO: Save method summary
    }

    @Override
    protected void flowThrough(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.clear();
        out.addAll(in);

        if (unit instanceof AssignStmt) {
            visitAssign(in, (AssignStmt) unit, out);
        }

        if (unit instanceof InvokeStmt) {
            visitInvoke(in, (InvokeStmt) unit, out);
        }

        if (unit instanceof ReturnStmt) {
            visitReturn(in, (ReturnStmt) unit, out);
        }

        if (unit instanceof ReturnVoidStmt) {
            visitReturnVoid(in, (ReturnVoidStmt) unit, out);
        }
    }

    private void visitAssign(Set<Taint> in, AssignStmt stmt, Set<Taint> out) {
        Value leftOp = stmt.getLeftOp();
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (configInterface.isGetter(invoke)) {
                Taint newTaint;
                if (stmtTaintCache.containsKey(stmt)) {
                    newTaint = stmtTaintCache.get(stmt);
                } else {
                    changed = true;
                    newTaint = new Taint(leftOp, stmt, method);
                    sources.add(newTaint);
                    stmtTaintCache.put(stmt, newTaint);
                }
                out.add(newTaint);
            } else {
                // TODO: Handle invoke
            }
        } else {
            // KILL
            for (Taint t : in) {
                if (t.taints(leftOp)) {
                    out.remove(t);
                }
            }

            // GEN
            for (Taint t : in) {
                for (ValueBox box : stmt.getUseBoxes()) {
                    Value value = box.getValue();
                    if (t.taints(value)) {
                        Taint newTaint;
                        if (stmtTaintCache.containsKey(stmt)) {
                            newTaint = stmtTaintCache.get(stmt);
                        } else {
                            changed = true;
                            newTaint = new Taint(leftOp, stmt, method);
                            stmtTaintCache.put(stmt, newTaint);
                        }
                        t.addSuccessor(newTaint);
                        out.add(newTaint);
                    }
                }
            }
        }
    }

    private void visitInvoke(Set<Taint> in, InvokeStmt stmt, Set<Taint> out) {
        out.clear();
    }

    private void visitReturn(Set<Taint> in, ReturnStmt stmt, Set<Taint> out) {
        out.clear();

        // Use body.getParameterLocals() to get the list of Locals representing the parameters (on LHS of IdentityStmt)

        // Use:
        //   if (!body.getMethod().isStatic()) {
        //      body.getThisLocal()
        // to get the Local representing @this

        // Use Taint t; Value v; t.taints(v) to check whether the taint abstraction taints value v
    }

    private void visitReturnVoid(Set<Taint> in, ReturnVoidStmt stmt, Set<Taint> out) {
        out.clear();
    }

    @Override
    protected Set<Taint> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected Set<Taint> entryInitialFlow() {
        return entryInitialFlow;
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
