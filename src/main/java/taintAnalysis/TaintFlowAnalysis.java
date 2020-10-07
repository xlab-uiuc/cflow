package taintAnalysis;

import configInterface.ConfigInterface;
import soot.Body;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaintFlowAnalysis extends ForwardFlowAnalysis<Unit, Set<Taint>> {

    private ConfigInterface configInterface;
    private List<Taint> sources;

    public TaintFlowAnalysis(Body body, ConfigInterface configInterface) {
        super(new ExceptionalUnitGraph(body));
        this.configInterface = configInterface;
        this.sources = new ArrayList<>();
        System.out.println(body.getMethod());
    }

    @Override
    protected void flowThrough(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.addAll(in);
        if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            for (Taint t : in) {
                for (ValueBox valueBox : rightOp.getUseBoxes()) {
                    if (t.getValue().equals(valueBox.getValue())) {
                        Taint newTaint = new Taint(t.getSource(), leftOp);
                        t.addSuccessor(newTaint);
                        out.add(newTaint);
                    }
                }
            }

            if (rightOp instanceof InvokeExpr) {
                InvokeExpr invoke = (InvokeExpr) rightOp;
                if (configInterface.isGetter(invoke)) {
                    Taint newTaint = new Taint(unit, leftOp);
                    sources.add(newTaint);
                    out.add(newTaint);
                }
            }
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
            dest.add(new Taint(t.getSource(), t.getValue(), t.getSuccessors()));
        }
    }

}
