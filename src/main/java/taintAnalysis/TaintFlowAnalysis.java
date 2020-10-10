package taintAnalysis;

import configInterface.ConfigInterface;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private final SootMethod method;
    private final ConfigInterface configInterface;
    private final List<Taint> sources;

    public TaintFlowAnalysis(Body body, ConfigInterface configInterface) {
        super(new ExceptionalUnitGraph(body));
        this.method = body.getMethod();
        this.configInterface = configInterface;
        this.sources = new ArrayList<>();
        System.out.println(body.getMethod());
    }

    @Override
    protected void flowThrough(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.addAll(in);
        Stmt stmt = (Stmt) unit;

        if (stmt instanceof IdentityStmt) {
        }

        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value leftOp = assignStmt.getLeftOp();

            if (stmt.containsInvokeExpr()) {
                InvokeExpr invoke = stmt.getInvokeExpr();
                if (configInterface.isGetter(invoke)) {
                    Taint newTaint = new Taint(leftOp, stmt, method);
                    sources.add(newTaint);
                    out.add(newTaint);
                } else {

                }
            } else {
                for (Taint t : in) {
                    // GEN
                    for (ValueBox box : stmt.getUseBoxes()) {
                        Value value = box.getValue();
                        if (t.taints(value)) {
                            Taint newTaint = new Taint(leftOp, stmt, method);
                            t.addSuccessor(newTaint);
                            out.add(newTaint);
                        }
                    }

                    // KILL
                    if (t.taints(leftOp)) {
                        out.remove(t);
                    }
                }
            }
        }

        if (stmt instanceof InvokeStmt) {
        }

        if (stmt instanceof ReturnStmt) {
        }

        if (stmt instanceof ReturnVoidStmt) {
        }
    }

    public void doAnalysis() {
        super.doAnalysis();
    }

    public List<Taint> getSources() {
        return sources;
    }

    @Override
    protected Set<Taint> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected void merge(Set<Taint> in1, Set<Taint> in2, Set<Taint> out) {
        out.addAll(in1);
        out.addAll(in2);
    }

    @Override
    protected void copy(Set<Taint> source, Set<Taint> dest) {
        for (Taint t : source) {
            dest.add(new Taint(t.getValue(), t.getStmt(), t.getMethod(), t.getSuccessors()));
        }
    }

}
