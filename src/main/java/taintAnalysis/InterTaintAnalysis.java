package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;

import java.util.*;

public class InterTaintAnalysis {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ISourceSinkManager sourceSinkManager;
    private final ITaintWrapper taintWrapper;
    private final List<Taint> sources;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;

    public InterTaintAnalysis(ISourceSinkManager sourceSinkManager, ITaintWrapper taintWrapper) {
        this.sourceSinkManager = sourceSinkManager;
        this.taintWrapper = taintWrapper;
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
        methodList.sort(Comparator.comparing(SootMethod::toString));

        logger.info("Num of methods: {}", methodList.size());

        // Bootstrap
        List<Body> bodyList = new ArrayList<>();
        for (SootMethod sm : methodList) {
            Body b = sm.retrieveActiveBody();
            bodyList.add(b);
        }
        for (Body b : bodyList) {
            TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, sourceSinkManager, Taint.getEmptyTaint(),
                    methodSummary, methodTaintCache, taintWrapper);
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
                    TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, sourceSinkManager, entryTaint,
                            methodSummary, methodTaintCache, taintWrapper);
                    analysis.doAnalysis();
                    changed |= analysis.isChanged();
                }
            }
            iter++;
        }
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
