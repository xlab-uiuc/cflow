package taintAnalysis;

import configInterface.ConfigInterface;
import soot.Body;
import soot.BodyTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntraAnalysisTransformer extends BodyTransformer {

    private ConfigInterface configInterface;

    private List<List<Taint>> sources;

    public IntraAnalysisTransformer(ConfigInterface configInterface) {
        this.configInterface = configInterface;
        sources = new ArrayList<>();
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        TaintFlowAnalysis analysis = new TaintFlowAnalysis(b, configInterface);
        analysis.doAnalysis();
        List<Taint> lst = analysis.getSources();
        if (lst.size() > 0)
            sources.add(lst);
//        for (Taint source : lst) {
//            System.out.println("source");
//            dfs(source, 0);
//        }
    }

    public List<List<Taint>> getSources() {
        return this.sources;
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
