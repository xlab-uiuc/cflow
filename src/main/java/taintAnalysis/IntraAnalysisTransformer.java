package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;

import java.util.*;

public class IntraAnalysisTransformer extends BodyTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigInterface configInterface;
    private final List<List<Taint>> sourceLists;
    private final Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary;
    private final Map<SootMethod, Map<Taint, Taint>> methodTaintCache;

    public IntraAnalysisTransformer(ConfigInterface configInterface) {
        this.configInterface = configInterface;
        this.sourceLists = Collections.synchronizedList(new ArrayList<>());
        this.methodSummary = new HashMap<>();
        this.methodTaintCache = new HashMap<>();
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
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
                new TaintFlowAnalysis(b, configInterface, Taint.getEmptyTaint(), methodSummary, methodTaintCache);
        analysis.doAnalysis();

        List<Taint> lst = analysis.getSources();
        sourceLists.add(lst);

        for (Taint source : lst) {
            System.out.println("source");
            dfs(source, 0);
        }
    }

    private void dfs(Taint t, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);
        for (Taint succ : t.getSuccessors()) {
            dfs(succ, depth + 1);
        }
    }

}
