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
        for (Taint source : analysis.getSources()) {
            System.out.println("source");

            HashSet<SootMethod> historyCallers = new HashSet<>();
            dfs(source, 0, historyCallers, null);
        }
    }

    private void dfs(Taint t, int depth, HashSet<SootMethod> historyCallers, SootMethod callerMethod) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        Stmt curStmt = t.getStmt();
        SootMethod recursiveCallee = null;
        if (curStmt.containsInvokeExpr()) {
            System.out.println(historyCallers);
            SootMethod callee = curStmt.getInvokeExpr().getMethod();
//            System.out.println("caller: " + t.getMethod() + " -> callee: " + callee);
            historyCallers.add(t.getMethod());
            if (historyCallers.contains(callee)) {
                recursiveCallee = callee;
            }
        }
        for (Taint succ : t.getSuccessors()) {
//            System.out.println("succ method: " + succ.getMethod() + " <<>> recur callee: " + recursiveCallee);
            if (succ.getMethod() != recursiveCallee) {
                if (curStmt instanceof InvokeStmt) {
                    dfs(succ, depth + 1, historyCallers, curStmt.getInvokeExpr().getMethod());
                } else if (curStmt instanceof ReturnStmt) {
                    if (callerMethod == succ.getMethod()) {
                        dfs(succ, depth + 1, historyCallers, null);
                    }
                } else {
                    dfs(succ, depth + 1, historyCallers, callerMethod);
                }
            }
        }
    }
}
