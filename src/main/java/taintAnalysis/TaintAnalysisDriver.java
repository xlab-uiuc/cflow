package taintAnalysis;

import configInterface.ConfigInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.PackManager;
import soot.Transform;

import java.util.List;

public class TaintAnalysisDriver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TaintAnalysisDriver() {
    }

    public IntraAnalysisTransformer runIntraTaintAnalysis(
            List<String> srcPaths, List<String> classPaths, ConfigInterface configInterface) {
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
                new Transform("jtp.taintanalysis", new IntraAnalysisTransformer(configInterface)));

        soot.Main.main(sootArgs);

        IntraAnalysisTransformer transformer = (IntraAnalysisTransformer)
                PackManager.v().getPack("jtp").get("jtp.taintanalysis").getTransformer();
        return transformer;
    }

    public InterAnalysisTransformer runInterTaintAnalysis(
            List<String> srcPaths, List<String> classPaths, ConfigInterface configInterface) {
        G.reset();

        String classPath = String.join(":", classPaths);
        String[] initArgs = {
                // General Options
                "-w",

                // Input Options
                "-cp", classPath,
                "-pp",
                "-allow-phantom-refs",
                "-no-bodies-for-excluded",

                // Phase Options
                "-p", "cg", "all-reachable",
                "-p", "cg.spark", "enabled",
                "-p", "cg.spark", "apponly",

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

        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.taintanalysis", new InterAnalysisTransformer(configInterface)));

        soot.Main.main(sootArgs);

        InterAnalysisTransformer transformer = (InterAnalysisTransformer)
                PackManager.v().getPack("wjtp").get("wjtp.taintanalysis").getTransformer();
        return transformer;
    }

}
