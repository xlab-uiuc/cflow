//package taintAnalysisTest;
//
//import common.ErrorMessage;
//import common.ErrorReport;
//import common.Utils;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.rules.Timeout;
//import soot.G;
//import soot.Scene;
//import soot.options.Options;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import static soot.SootClass.SIGNATURES;
//
//public class TaintAnalysisTest {
//    private String testClass;
//    String analysisName;
//    Set<ErrorReport> expected;
//
//    @Before
//    public void initTest() {
//        System.out.println("Initializing tests");
//        G.reset();
//        Utils.resetErrors();
//        expected = new HashSet<>();
//        Options.v().set_keep_line_number(true);
//        Options.v().set_output_format(Options.output_format_J);
//        Options.v().set_prepend_classpath(true);
//        Options.v().set_output_dir("sootOutput");
//        String sep = System.getProperty("file.separator");
//        String sootClasspath = "build" + sep + "classes" + sep + "java" + sep + "test";
//        Options.v().set_soot_classpath(sootClasspath);
//        add_analysis();
//        System.out.println("Done initializing");
//    }
//
//    abstract void add_analysis();
//
//    String[] getArgs() {
//        List<String> srcPaths = new ArrayList<>();
//        srcPaths.add("Test.jar");
//
//        String classPath = String.join(":", srcPaths);
//        String[] initArgs = {
//                // Input Options
//                "-cp", classPath,
//                "-pp",
//                "-allow-phantom-refs",
//                "-no-bodies-for-excluded",
//
//                // Output Options
//                "-f", "J",
//        };
//
//        String[] sootArgs = new String[initArgs.length + 2 * srcPaths.size()];
//        for (int i = 0; i < initArgs.length; i++) {
//            sootArgs[i] = initArgs[i];
//        }
//        for (int i = 0; i < srcPaths.size(); i++) {
//            sootArgs[initArgs.length + 2*i] = "-process-dir";
//            sootArgs[initArgs.length + 2*i + 1] = srcPaths.get(i);
//        }
//
//        return sootArgs;
//    }
//
//    void addTestClass(String testClass) {
//        this.testClass = testClass;
//        Scene.v().addBasicClass(testClass, SIGNATURES);
//        Scene.v().loadBasicClasses();
//        Scene.v().loadNecessaryClasses();
//    }
//
//    void addExpected(ErrorMessage m, int line) {
//        expected.add(new ErrorReport(m, line));
//    }
//}
