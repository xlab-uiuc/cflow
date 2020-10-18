package taintAnalysis;

import configInterface.ConfigInterface;
import soot.Body;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;

import java.util.*;

public class IntraAnalysisTransformer extends BodyTransformer {

    private ConfigInterface configInterface;

    private List<List<Taint>> sources;

    private Map<SootMethod, List<Set<Taint>>> methodSummary;

    public IntraAnalysisTransformer(ConfigInterface configInterface) {
        this.configInterface = configInterface;
        sources = new ArrayList<>();
        methodSummary = new HashMap<>();
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, configInterface);
        try {
            analysis.doAnalysis();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        List<Taint> lst = analysis.getSources();
        Map<SootMethod, List<Set<Taint>>> summary = analysis.getMethodSummary();
        if (lst.size() > 0)
            sources.add(lst);
        if (summary.size() > 0)
            methodSummary.putAll(summary);

        for (Taint source : lst) {
            System.out.println("source");
            dfs(source, 0);
        }
    }

    public List<List<Taint>> getSources() {
        return this.sources;
    }

    public Map<SootMethod, List<Set<Taint>>> getMethodSummary() {
        return methodSummary;
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
