package taintAnalysis;

import configInterface.ConfigInterface;
import configInterface.TestInterface;
import soot.Body;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.*;

public class InterTaintAnalysis {

    private final CallGraph cg;
    private final ConfigInterface configInterface;
    private final List<Taint> sources;
    private final Map<SootMethod, Map<Taint, Set<Taint>>> methodSummary;
    private final Map<SootMethod, Map<Stmt, Taint>> taintCache;

    public InterTaintAnalysis(CallGraph cg, ConfigInterface configInterface) {
        this.cg = cg;
        this.configInterface = configInterface;
        this.sources = new ArrayList<>();
        this.methodSummary = new HashMap<>();
        this.taintCache = new HashMap<>();
    }

    public List<Taint> getSources() {
        return sources;
    }

    public void doAnalysis() {
        boolean changed = true;

        List<SootMethod> lst = new ArrayList<>();

        while (changed) {
            for (SootMethod sm : lst) {
                if (!sm.hasActiveBody()) {
                    continue;
                }
                Body b = sm.getActiveBody();
                TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, new TestInterface());
                analysis.doAnalysis();
                changed |= analysis.isChanged();
            }
        }
    }

}
