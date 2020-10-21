package taintAnalysis;

import configInterface.ConfigInterface;
import configInterface.HadoopInterface;
import configInterface.TestInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.PackManager;
import soot.Transform;

import java.util.ArrayList;
import java.util.List;

public class TaintAnalysisDriver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TaintAnalysisDriver() {
    }

    public IntraAnalysisTransformer runIntraTaintAnalysis() {
        List<String> srcPaths = new ArrayList<>();
        System.out.println("running Test.jar");
        srcPaths.add("Test/out/artifacts/Test_jar/Test.jar");

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

        IntraAnalysisTransformer transformer = (IntraAnalysisTransformer)
                PackManager.v().getPack("jtp").get("jtp.taintanalysis").getTransformer();
        return transformer;
    }

    public InterAnalysisTransformer runInterTaintAnalysis() {
        List<String> srcPaths = new ArrayList<>();
        System.out.println("running Test.jar");
        srcPaths.add("Test/out/artifacts/Test_jar/Test.jar");

        String classPath = String.join(":", srcPaths);
        String[] initArgs = {
                // Input Options
                "-cp", classPath,
                "-pp",
                "-allow-phantom-refs",
                "-no-bodies-for-excluded",

                // Phase Options
                "-w",

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
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.taintanalysis", new InterAnalysisTransformer(configInterface)));

        soot.Main.main(sootArgs);

        InterAnalysisTransformer transformer = (InterAnalysisTransformer)
                PackManager.v().getPack("wjtp").get("wjtp.taintanalysis").getTransformer();
        return transformer;
    }

    public List<List<Taint>> runHadoop() {
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
                new Transform("jtp.hadooptaintanalysis", new IntraAnalysisTransformer(configInterface)));

        try {
            soot.Main.main(sootArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        IntraAnalysisTransformer transformer = (IntraAnalysisTransformer) PackManager.v().getPack("jtp").get("jtp.hadooptaintanalysis").getTransformer();
        return transformer.getSourceLists();
    }

}



