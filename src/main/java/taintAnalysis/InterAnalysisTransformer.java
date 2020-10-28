package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterAnalysisTransformer extends SceneTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InterTaintAnalysis analysis;

    public InterAnalysisTransformer(ConfigInterface configInterface) {
        this.analysis = new InterTaintAnalysis(Scene.v(), configInterface);
    }

    public ConfigInterface getConfigInterface() {
        return analysis.getConfigInterface();
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
        Set<Taint> visited = new HashSet<>();
        for (Taint source : analysis.getSources()) {
            System.out.println("source");
            dfs(source, 0, visited);
        }
    }

    private void dfs(Taint t, int depth, Set<Taint> visited) {
        if (visited.contains(t)) {
            return;
        }
        visited.add(t);
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);
        for (Taint succ : t.getSuccessors()) {
            dfs(succ, depth + 1, visited);
        }
    }

}
