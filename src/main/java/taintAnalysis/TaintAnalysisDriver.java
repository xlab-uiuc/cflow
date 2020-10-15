package taintAnalysis;

import configInterface.ConfigInterface;
import configInterface.HadoopInterface;
import configInterface.TestInterface;
import fj.data.hlist.HPre;
import soot.PackManager;
import soot.Transform;

import java.util.ArrayList;
import java.util.List;

public class TaintAnalysisDriver {

    public TaintAnalysisDriver() {
    }

    public List<List<Taint>> run() {
        List<String> srcPaths = new ArrayList<>();
        srcPaths.add("Test.jar");

        String classPath = String.join(":", srcPaths);
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

        ConfigInterface configInterface = new TestInterface();
        PackManager.v().getPack("jtp").add(
                new Transform("jtp.taintanalysis", new IntraAnalysisTransformer(configInterface)));

        soot.Main.main(sootArgs);

        IntraAnalysisTransformer transformer = (IntraAnalysisTransformer) PackManager.v().getPack("jtp").get("jtp.taintanalysis").getTransformer();
        return transformer.getSources();
    }

    public void runHadoop() {
        String hadoopJar = "app/hadoop-3.3.0/share/hadoop/common/hadoop-common-3.3.0.jar";
        List<String> srcPaths = new ArrayList<>();
        srcPaths.add(hadoopJar);

        String classPath = String.join(":", srcPaths);
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

        ConfigInterface configInterface = new HadoopInterface();
        PackManager.v().getPack("jtp").add(
                new Transform("jtp.taintanalysis", new IntraAnalysisTransformer(configInterface)));

        try {
            soot.Main.main(sootArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}



