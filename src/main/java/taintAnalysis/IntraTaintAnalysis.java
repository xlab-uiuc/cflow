package taintAnalysis;

import configInterface.ConfigInterface;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.SimpleDominatorsFinder;

import java.util.*;

    public class IntraTaintAnalysis {

    private final ExceptionalBlockGraph graph;
    private final SimpleDominatorsFinder<Block> dominatorsFinder;
    private final SootMethod method;
    private final ConfigInterface configInterface;
    private final List<Taint> sources;
    private final Map<Block, Set<Taint>> blockTaintMap;

    public IntraTaintAnalysis(Body body, ConfigInterface configInterface) {
        this.graph = new ExceptionalBlockGraph(body);
        this.dominatorsFinder = new SimpleDominatorsFinder<>(graph);
        this.method = body.getMethod();
        this.configInterface = configInterface;
        this.sources = new ArrayList<>();
        this.blockTaintMap = new HashMap<>();
        System.out.println(body.getMethod());
    }

    public List<Taint> getSources() {
        return sources;
    }

    public void doAnalysis() {
        for (Block block : graph) {
            visitBlock(block);
        }
    }

    private void visitBlock(Block block) {
        Set<Taint> in = new HashSet<>();
        Set<Taint> out = new HashSet<>();

        // Preprocessing: merge out from preds, skipping back edges
        for (Block pred : graph.getUnexceptionalPredsOf(block)) {
            if (dominatorsFinder.isDominatedBy(pred, block))
                continue;
            in.addAll(blockTaintMap.get(pred));
        }
        for (Block pred : graph.getExceptionalPredsOf(block)) {
            if (dominatorsFinder.isDominatedBy(pred, block))
                continue;
            in.addAll(blockTaintMap.get(pred));
        }

        for (Unit unit : block) {
            visitUnit(in, unit, out);
            copy(out, in);
        }

        // Postprocessing: propagate results back along back edges
        for (Block succ : graph.getSuccsOf(block)) {
            if (dominatorsFinder.isDominatedBy(block, succ)) {
                blockTaintMap.get(succ).addAll(out);
            }
        }
        for (Block succ : graph.getExceptionalSuccsOf(block)) {
            if (dominatorsFinder.isDominatedBy(block, succ)) {
                blockTaintMap.get(succ).addAll(out);
            }
        }

        blockTaintMap.put(block, out);
    }

    private void visitUnit(Set<Taint> in, Unit unit, Set<Taint> out) {
        out.clear();
        out.addAll(in);

        if (unit instanceof IdentityStmt) {
        }

        if (unit instanceof AssignStmt) {
            visitAssign(in, (AssignStmt) unit, out);
        }

        if (unit instanceof InvokeStmt) {
        }

        if (unit instanceof ReturnStmt) {
        }

        if (unit instanceof ReturnVoidStmt) {
        }
    }

    private void visitAssign(Set<Taint> in, AssignStmt stmt, Set<Taint> out) {
        Value leftOp = stmt.getLeftOp();
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

    private void copy(Set<Taint> source, Set<Taint> dest) {
        dest.clear();
        dest.addAll(source);
    }
}
