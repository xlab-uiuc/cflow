package taintAnalysis;

import configInterface.ConfigInterface;
import configInterface.TestInterface;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.*;

public class InterTaintAnalysis {

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

        List<SootMethod> lst = new ArrayList<>();
        for (SootClass sc : scene.getApplicationClasses()) {
            lst.addAll(sc.getMethods());
        }

        for (SootMethod sm : lst) {
            if (!sm.hasActiveBody()) {
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
            if (iter > 5)
                break;

            for (SootMethod sm : lst) {
                if (!sm.hasActiveBody()) {
                    continue;
                }
                Body b = sm.getActiveBody();
                for (Taint entryTaint : methodSummary.get(sm).keySet()) {
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
