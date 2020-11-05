package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        for (Taint source : analysis.getSources()) {
            Set<Taint> visited = new HashSet<>();
            System.out.println("source");
            dfs(source, 0, visited, null);
        }
    }

    private void dfs(Taint t, int depth, Set<Taint> visited, SootMethod callerMethod) {
        if (visited.contains(t)) {
            return;
        }
        visited.add(t);
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);
        Stmt curStmt = t.getStmt();
        for (Taint succ : t.getSuccessors()) {
            if (curStmt instanceof InvokeStmt) {
                dfs(succ, depth + 1, visited, curStmt.getInvokeExpr().getMethod());
            } else if (curStmt instanceof ReturnStmt) {
                if (callerMethod == succ.getMethod()) {
                    dfs(succ, depth + 1, visited, null);
                }
            } else {
                dfs(succ, depth + 1, visited, callerMethod);
            }
        }
    }
}
