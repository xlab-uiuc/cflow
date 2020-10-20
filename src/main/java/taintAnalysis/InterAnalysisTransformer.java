package taintAnalysis;

import configInterface.ConfigInterface;
import soot.Scene;
import soot.SceneTransformer;

import java.util.Map;

public class InterAnalysisTransformer extends SceneTransformer {

    private ConfigInterface configInterface;

    public InterAnalysisTransformer(ConfigInterface configInterface) {
        this.configInterface = configInterface;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        InterTaintAnalysis analysis = new InterTaintAnalysis(Scene.v(), configInterface);
        analysis.doAnalysis();

        for (Taint source : analysis.getSources()) {
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
