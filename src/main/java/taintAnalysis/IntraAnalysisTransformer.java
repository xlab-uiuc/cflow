package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;

import java.util.*;

public class IntraAnalysisTransformer extends BodyTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ISourceSinkManager sourceSinkManager;
    private final ITaintWrapper taintWrapper;
    private final List<List<Taint>> sourceLists;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;

    public IntraAnalysisTransformer(ISourceSinkManager sourceSinkManager, ITaintWrapper taintWrapper) {
        this.sourceSinkManager = sourceSinkManager;
        this.taintWrapper = taintWrapper;
        this.sourceLists = Collections.synchronizedList(new ArrayList<>());
        this.methodSummary = new HashMap<>();
        this.methodTaintCache = new HashMap<>();
    }

    public List<List<Taint>> getSourceLists() {
        return sourceLists;
    }

    public Map<SootMethod, Map<Taint, List<Set<Taint>>>> getMethodSummary() {
        return methodSummary;
    }

    public Map<SootMethod, Map<Taint, Taint>> getMethodTaintCache() {
        return methodTaintCache;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        TaintFlowAnalysis analysis =
                new TaintFlowAnalysis(b, sourceSinkManager, Taint.getEmptyTaint(),
                        methodSummary, methodTaintCache, taintWrapper);
        analysis.doAnalysis();

        List<Taint> lst = new ArrayList<>();
        lst.addAll(analysis.getSources());
        sourceLists.add(lst);
    }

}
