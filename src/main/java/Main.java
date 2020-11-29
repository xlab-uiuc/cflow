import assertion.Assert;
import configInterface.ConfigInterface;
import org.apache.commons.cli.*;
import soot.SootMethod;
import taintAnalysis.InterAnalysisTransformer;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.sourceSinkManager.SourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;
import taintAnalysis.taintWrapper.TaintWrapper;
import utility.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option optionA = Option.builder("o")
                .required(false)
                .desc("This parameter specifies the exported file path. If not specified, output will be directed to stdout.")
                .longOpt("output")
                .hasArg()
                .build();

        Option optionB = Option.builder("x")
                .required(true)
                .desc("Configuration parameter directory path.")
                .longOpt("xml")
                .hasArg()
                .build();

        Option optionC = Option.builder("a")
                .required(true)
                .desc("Support applications are: test, hdfs, mapreduce, yarn, hadoop_common, hadoop_tools, hbase, alluxio, zookeeper, spark")
                .longOpt("app")
                .hasArg()
                .build();

        Option optionD = new Option("intra", "Run intra-procedural analysis (testing only)");

        options.addOption(optionA);
        options.addOption(optionB);
        options.addOption(optionC);
        options.addOption(optionD);

        try {
            CommandLine commandLine = parser.parse(options, args);
            boolean intra = false;

            /* getting optional parameters */
            if (commandLine.hasOption('o')) {
                /* getting option o */
                String filePath = commandLine.getOptionValue('o');
                PrintStream fileOut = new PrintStream(new FileOutputStream(filePath, true));
                System.setOut(fileOut);
            }

            if (commandLine.hasOption("intra")) {
                /* getting option intra */
                intra = true;
            }

            /* getting required parameters */
            /* getting option a */
            String apps = commandLine.getOptionValue('a');
            String[] result = apps.split(",");
            String[][] considered = new String[result.length][];
            for (int i = 0; i < considered.length; i++) {
                try {
                    considered[i] = Config.getCfg(result[i]);
                } catch (IllegalArgumentException e) {
                    throw new ParseException(result[i] + " not found in supported application");
                }
            }

            run(considered, intra);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            new HelpFormatter().printHelp("ccc", options);
        }
    }

    private static void run(String[][] considered, boolean intra) throws IOException {
        List<String> srcPaths = new LinkedList<>();
        List<String> classPaths = new LinkedList<>();
        ConfigInterface configInterface = null;

        for (String[] cfg : considered) {
            srcPaths.addAll(Config.getSourcePaths(cfg));
            classPaths.addAll(Config.getClassPaths(cfg));
            configInterface = Config.getInterface(cfg);
        }

        // Run taint analysis
        ISourceSinkManager sourceSinkManager = new SourceSinkManager(configInterface);
//        ITaintWrapper taintWrapper = TaintWrapper.getDefault();
        TaintAnalysisDriver driver = new TaintAnalysisDriver(sourceSinkManager, null);
        if (intra) {
            driver.runIntraTaintAnalysis(srcPaths, classPaths);
        } else {
            driver.runInterTaintAnalysis(srcPaths, classPaths);
        }
//
//        InterAnalysisTransformer transformer1 = driver.runInterTaintAnalysis(srcPaths, classPaths);
//        InterAnalysisTransformer transformer2 = driver.runInterTaintAnalysis(srcPaths, classPaths);
//
//
//
//        ArrayList<SootMethod> lst1 = new ArrayList<>(transformer1.getMethodTaintCache().keySet());
//        lst1.sort(Comparator.comparing(SootMethod::toString));
//        ArrayList<SootMethod> lst2 = new ArrayList<>(transformer2.getMethodTaintCache().keySet());
//        lst2.sort(Comparator.comparing(SootMethod::toString));
//
//        Assert.assertEquals(lst1.size(), lst2.size());
//        for (int i = 0; i < lst1.size(); i++) {
//            SootMethod sm1 = lst1.get(i);
//            SootMethod sm2 = lst2.get(i);
//            Map<Taint, Taint> s1 = transformer1.getMethodTaintCache().get(sm1);
//            Map<Taint, Taint> s2 = transformer2.getMethodTaintCache().get(sm2);
//            Assert.assertEquals(s1.size(), s2.size());
//            if (s1.isEmpty()) {
//                continue;
//            }
//            ArrayList<Taint> l1 = new ArrayList<>(s1.values());
//            l1.sort(Comparator.comparing(Taint::toString));
//            ArrayList<Taint> l2 = new ArrayList<>(s2.values());
//            l2.sort(Comparator.comparing(Taint::toString));
//            Assert.assertEquals(l1.size(), l2.size());
//            for (int j = 0; j < l1.size(); j++) {
//                Taint t1 = l1.get(j);
//                Taint t2 = l2.get(j);
//                isSame(t1, t2, sm1);
//            }
//        }
//
//        System.out.println("Method Summary");
//        ArrayList<SootMethod> lstsummary1 = new ArrayList<>(transformer1.getMethodSummary().keySet());
//        lstsummary1.sort(Comparator.comparing(SootMethod::toString));
//        ArrayList<SootMethod> lstsummary2 = new ArrayList<>(transformer2.getMethodSummary().keySet());
//        lstsummary2.sort(Comparator.comparing(SootMethod::toString));
//
//        Assert.assertEquals(lstsummary1.size(), lstsummary2.size());
//        for (int i = 0; i < lstsummary1.size(); i++) {
//            SootMethod sm1 = lstsummary1.get(i);
//            SootMethod sm2 = lstsummary2.get(i);
//            Map<Taint, List<Set<Taint>>> s1 = transformer1.getMethodSummary().get(sm1);
//            Map<Taint, List<Set<Taint>>> s2 = transformer2.getMethodSummary().get(sm2);
//            if (s1.size() != s2.size()) {
//                System.out.println("summary size");
//                System.out.println(sm1);
//                System.out.println(sm2);
//                System.out.println(s1.size());
//                System.out.println(s2.size());
//                for (Taint t : s1.keySet()) {
//                    System.out.println(" ->" + t);
//                }
//                for (Taint t : s2.keySet()) {
//                    System.out.println(" ->" + t);
//                }
//                continue;
//            }
//            if (s1.isEmpty()) {
//                continue;
//            }
//            ArrayList<Taint> l1 = new ArrayList<>(s1.keySet());
//            l1.sort(Comparator.comparing(Taint::toString));
//            ArrayList<Taint> l2 = new ArrayList<>(s2.keySet());
//            l2.sort(Comparator.comparing(Taint::toString));
//            Assert.assertEquals(l1.size(), l2.size());
//            for (int j = 0; j < l1.size(); j++) {
//                Taint t1 = l1.get(j);
//                Taint t2 = l2.get(j);
//                isSame(t1, t2, sm1);
//                if (t1.isEmpty()) {
//                    continue;
//                }
//                if (s1.get(t1).size() != s2.get(t2).size()) {
//                    System.out.println("C");
//                    System.out.println(sm1);
//                    System.out.println(t1);
//                    System.out.println(t2);
//                    System.out.println(s1.get(t1).size());
//                    System.out.println(s2.get(t2).size());
//                }
//                Assert.assertEquals(s1.get(t1).size(), sm1.getParameterCount() + 2);
//                Assert.assertEquals(s2.get(t2).size(), sm2.getParameterCount() + 2);
//                for (int k = 0; k < s1.get(t1).size(); k++) {
//                    Set<Taint> st1 = s1.get(t1).get(k);
//                    Set<Taint> st2 = s2.get(t2).get(k);
//                    ArrayList<Taint> at1 = new ArrayList<>(st1);
//                    at1.sort(Comparator.comparing(Taint::toString));
//                    ArrayList<Taint> at2 = new ArrayList<>(st2);
//                    at2.sort(Comparator.comparing(Taint::toString));
//                    Assert.assertEquals(at1.size(), at2.size());
//                    for (int l = 0; l < at1.size(); l++) {
//                        isSame(at1.get(l), at2.get(l), sm1);
//                    }
//                }
//            }
//        }

    }

    private static void isSame(Taint t1, Taint t2, SootMethod sm) {
        if (t1.toString().compareTo(t2.toString()) !=0) {
            System.out.println("A");
            System.out.println(sm);
            System.out.println(t1);
            System.out.println(t2);
            return;
        }
        for (Taint ta: t1.getSuccessors()) {
            boolean found = false;
            for (Taint tb: t2.getSuccessors()) {
                if (ta.toString().compareTo(tb.toString()) ==0) {
                    found = true;
                }
            }
            if (found == false) {
                System.out.println("B");
                System.out.println(sm);
                System.out.println(t1);
                for (Taint t11: t1.getSuccessors()) {
                    System.out.println(" ->" + t11);
                }
                System.out.println(t2);
                for (Taint t21: t2.getSuccessors()) {
                    System.out.println(" ->" + t21);
                }
                break;
            }
        }
    }


}
