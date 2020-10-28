package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;

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

        List<SootMethod> methodList = new ArrayList<>();
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                if (sm.isConcrete()) {
                    methodList.add(sm);
                }
            }
        }

        logger.info("Num of methods: {}", methodList.size());
        for (SootMethod sm : methodList) {
            Body b = sm.retrieveActiveBody();
            TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, configInterface, Taint.getEmptyTaint(), methodSummary, methodTaintCache);
            analysis.doAnalysis();
            sources.addAll(analysis.getSources());
        }

        int iter = 0;
        while (changed) {
            changed = false;
            System.out.println("iter" + iter);

            for (SootMethod sm : methodList) {
                Body b = sm.retrieveActiveBody();
                Set<Taint> entryTaints = new HashSet<>();
                entryTaints.addAll(methodSummary.get(sm).keySet());
                for (Taint entryTaint : entryTaints) {
                    TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, configInterface, entryTaint, methodSummary, methodTaintCache);
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
