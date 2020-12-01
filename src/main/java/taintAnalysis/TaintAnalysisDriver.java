package taintAnalysis;

import soot.G;
import soot.PackManager;
import soot.Transform;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;

import java.util.List;

public class TaintAnalysisDriver {

    private ISourceSinkManager sourceSinkManager;
    private ITaintWrapper taintWrapper;

    public TaintAnalysisDriver(ISourceSinkManager sourceSinkManager) {
        this(sourceSinkManager, null);
    }

    public TaintAnalysisDriver(ISourceSinkManager sourceSinkManager, ITaintWrapper taintWrapper) {
        this.sourceSinkManager = sourceSinkManager;
        this.taintWrapper = taintWrapper;
    }

    public IntraAnalysisTransformer runIntraTaintAnalysis(List<String> srcPaths, List<String> classPaths) {
        G.reset();

        String classPath = String.join(":", classPaths);
        String[] initArgs = {
                // Input Options
                "-cp", classPath,
                "-pp",
                "-allow-phantom-refs",
                "-no-bodies-for-excluded",

                // Output Options
                "-f", "J",
        };

        String[] sootArgs = new String[initArgs.length + 2 * srcPaths.size()];
        for (int i = 0; i < initArgs.length; i++) {
            sootArgs[i] = initArgs[i];
        }
        for (int i = 0; i < srcPaths.size(); i++) {
            sootArgs[initArgs.length + 2*i] = "-process-dir";
            sootArgs[initArgs.length + 2*i + 1] = srcPaths.get(i);
        }

        PackManager.v().getPack("jtp").add(
                new Transform("jtp.taintanalysis", new IntraAnalysisTransformer(sourceSinkManager, taintWrapper)));

        soot.Main.main(sootArgs);

        IntraAnalysisTransformer transformer = (IntraAnalysisTransformer)
                PackManager.v().getPack("jtp").get("jtp.taintanalysis").getTransformer();
        return transformer;
    }

    public InterAnalysisTransformer runInterTaintAnalysis(List<String> srcPaths, List<String> classPaths, boolean use_spark) {
        G.reset();

        String classPath = String.join(":", classPaths);
        String[] initArgs;
        if (use_spark) {
            initArgs = new String[]{
                    // General Options
                    "-w",

                    // Input Options
                    "-cp", classPath,
                    "-pp",
                    "-allow-phantom-refs",
                    "-no-bodies-for-excluded",

                    // Output Options
                    "-f", "J",

                    // Phase Options
                    "-p", "cg", "all-reachable",
                    "-p", "cg.spark", "enabled",
                    "-p", "cg.spark", "apponly"
            };
        } else {
            initArgs = new String[]{
                    // General Options
                    "-w",

                    // Input Options
                    "-cp", classPath,
                    "-pp",
                    "-allow-phantom-refs",
                    "-no-bodies-for-excluded",

                    // Output Options
                    "-f", "J",

                    // Phase Options
                    "-p", "cg", "off"
            };
        }

        String[] sootArgs = new String[initArgs.length + 2 * srcPaths.size()];
        for (int i = 0; i < initArgs.length; i++) {
            sootArgs[i] = initArgs[i];
        }
        for (int i = 0; i < srcPaths.size(); i++) {
            sootArgs[initArgs.length + 2*i] = "-process-dir";
            sootArgs[initArgs.length + 2*i + 1] = srcPaths.get(i);
        }

        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.taintanalysis", new InterAnalysisTransformer(sourceSinkManager, taintWrapper)));

        soot.Main.main(sootArgs);

        InterAnalysisTransformer transformer = (InterAnalysisTransformer)
                PackManager.v().getPack("wjtp").get("wjtp.taintanalysis").getTransformer();
        return transformer;
    }

    public ISourceSinkManager getSourceSinkManager() {
        return sourceSinkManager;
    }

    public void setSourceSinkManager(ISourceSinkManager sourceSinkManager) {
        this.sourceSinkManager = sourceSinkManager;
    }

    public ITaintWrapper getTaintWrapper() {
        return taintWrapper;
    }

    public void setTaintWrapper(ITaintWrapper taintWrapper) {
        this.taintWrapper = taintWrapper;
    }

}
