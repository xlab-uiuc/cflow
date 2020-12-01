import configInterface.ConfigInterface;
import org.apache.commons.cli.*;
import soot.SootMethod;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.sourceSinkManager.SourceSinkManager;
import taintAnalysis.taintWrapper.ITaintWrapper;
import taintAnalysis.taintWrapper.TaintWrapper;
import utility.Config;

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
        ITaintWrapper taintWrapper = TaintWrapper.getDefault();
        TaintAnalysisDriver driver = new TaintAnalysisDriver(sourceSinkManager, taintWrapper);
        if (intra) {
            driver.runIntraTaintAnalysis(srcPaths, classPaths);
        } else {
            driver.runInterTaintAnalysis(srcPaths, classPaths);
        }
    }

}
