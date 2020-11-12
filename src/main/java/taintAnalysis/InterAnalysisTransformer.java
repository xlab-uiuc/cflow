package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;

import java.util.*;

public class InterAnalysisTransformer extends SceneTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InterTaintAnalysis analysis;

    public InterAnalysisTransformer(ISourceSinkManager sourceSinkManager, ITaintWrapper taintWrapper) {
        this.analysis = new InterTaintAnalysis(Scene.v(), sourceSinkManager, taintWrapper);
    }

    public List<Taint> getSources() {
        return analysis.getSources();
    }

    public Map<SootMethod, Map<Taint, List<Set<Taint>>>> getMethodSummary() {
        return analysis.getMethodSummary();
    }

    public Map<SootMethod, Map<Taint, Taint>> getMethodTaintCache() {
        return analysis.getMethodTaintCache();
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        analysis.doAnalysis();

        logger.info("Number of sources: {}", analysis.getSources().size());
        ArrayList<Taint> lst = new ArrayList<>(analysis.getSources());
        lst.sort(Comparator.comparing(Taint::toString));
        for (Taint source : analysis.getSources()) {
            System.out.println("source");
            PathVisitor pathVisitor = new PathVisitor();
            pathVisitor.visit(source);
        }
    }

}
