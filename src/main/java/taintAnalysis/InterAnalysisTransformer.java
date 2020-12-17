package taintAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SceneTransformer;
import soot.SootMethod;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;
import taintAnalysis.utility.PhantomIdentityStmt;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InterAnalysisTransformer extends SceneTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InterTaintAnalysis analysis;
    private boolean printResults = true;
    private Map<Taint, List<List<Taint>>> pathsMap = new HashMap<>();

    public InterAnalysisTransformer(ISourceSinkManager sourceSinkManager, ITaintWrapper taintWrapper) {
        this.analysis = new InterTaintAnalysis(sourceSinkManager, taintWrapper);
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

        Set<Taint> sinks = new HashSet<>();
        ArrayList<Taint> sources = new ArrayList<>(analysis.getSources());
        sources.sort(Comparator.comparing(Taint::toString));

        // // For validation only
        // PathVisitor pv = new PathVisitor();
        // for (Taint source : sources) {
        //     pv.visit(source);
        // }

       int numOfThread = 5;
       logger.info("Reconstructing path using {} threads...", numOfThread);
       ExecutorService es = Executors.newFixedThreadPool(numOfThread);
       List<SourceSinkConnectionVisitor> todo = new ArrayList<>(sources.size());
       for (Taint source : sources) {
           todo.add(new SourceSinkConnectionVisitor(source));
       }
       try {
           es.invokeAll(todo);
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       for (SourceSinkConnectionVisitor pv : todo) {
           pathsMap.put(pv.getSource(), pv.getPaths());
           sinks.addAll(pv.getSinks());
       }
       es.shutdown();

       logger.info("Number of sinks reached by path reconstruction: {}", sinks.size());

       if (printResults) {
           logger.info("Printing results...");
           for (Taint source : sources) {
               System.out.println("Source: " + source + " reaches:\n");
               List<List<Taint>> paths = pathsMap.get(source);
               for (List<Taint> path : paths) {
                   System.out.println("-- Sink " + path.get(path.size() - 1) + " along:");
                   for (Taint t : path) {
                       if (t.getStmt() instanceof PhantomIdentityStmt ||
                               t.getStmt() instanceof PhantomRetStmt)
                           continue;
                       System.out.println("    -> " + t);
                   }
                   System.out.println();
               }
               System.out.println();
           }
       }
    }

    public Map<Taint, List<List<Taint>>> getPathsMap() {
        return pathsMap;
    }

}
