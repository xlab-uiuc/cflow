package taintAnalysis;

import configInterface.ConfigInterface;
import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.Map;

public class InterAnalysisTransformer extends SceneTransformer {

    private ConfigInterface configInterface;

    public InterAnalysisTransformer(ConfigInterface configInterface) {
        this.configInterface = configInterface;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        CallGraph cg = Scene.v().getCallGraph();
        InterTaintAnalysis analysis = new InterTaintAnalysis(cg, configInterface);
    }

}
