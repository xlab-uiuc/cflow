package taintAnalysis;

import configInterface.ConfigInterface;
import configInterface.TestInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.TopologicalOrderer;

import java.util.*;

public class InterTaintAnalysis {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Scene scene;
    private final ConfigInterface configInterface;
    private final List<Taint> sources;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;

    public InterTaintAnalysis(Scene scene, ConfigInterface configInterface) {
        this.scene = scene;
        this.configInterface = configInterface;
        this.sources = new ArrayList<>();
        this.methodSummary = new HashMap<>();
        this.methodTaintCache = new HashMap<>();
    }

    public void doAnalysis() {
        this.sources.clear();
        this.methodSummary.clear();
        this.methodTaintCache.clear();

        boolean changed = true;

        // Traverse the methods in reverse post-order
        CallGraph cg = Scene.v().getCallGraph();
        TopologicalOrderer orderer = new TopologicalOrderer(cg);
        orderer.go();
        List<SootMethod> lst = orderer.order();
        Collections.reverse(lst);

        for (SootMethod sm : lst) {
            if (!sm.hasActiveBody()) {
                System.out.println(sm);
                continue;
            }
            Body b = sm.getActiveBody();
            TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, new TestInterface(), Taint.getEmptyTaint(), methodSummary, methodTaintCache);
            analysis.doAnalysis();
            sources.addAll(analysis.getSources());
        }

        int iter = 0;
        while (changed) {
            changed = false;
            System.out.println("iter" + iter);
            if (iter > 10)
                break;

            for (SootMethod sm : lst) {
                if (!sm.hasActiveBody()) {
                    System.out.println(sm);
                    continue;
                }
                Body b = sm.getActiveBody();
                Set<Taint> entryTaints = new HashSet<>();
                entryTaints.addAll(methodSummary.get(sm).keySet());
                for (Taint entryTaint : entryTaints) {
                    TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, new TestInterface(), entryTaint, methodSummary, methodTaintCache);
                    analysis.doAnalysis();
                    changed |= analysis.isChanged();
                }
            }
            iter++;
        }

    }

    public Scene getScene() {
        return scene;
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
    }

    public List<Taint> getSources() {
        return sources;
    }

    public Map<SootMethod, Map<Taint, List<Set<Taint>>>> getMethodSummary() {
        return methodSummary;
    }

    public Map<SootMethod, Map<Taint, Taint>> getMethodTaintCache() {
        return methodTaintCache;
    }

}
