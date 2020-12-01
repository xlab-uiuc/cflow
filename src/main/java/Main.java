import configInterface.ConfigInterface;
import org.apache.commons.cli.*;
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
        Option optionApp = Option.builder("a")
                .required(true)
                .desc("Support applications are: test, hdfs, mapreduce, yarn, hadoop_common, hadoop_tools, hbase, alluxio, zookeeper, spark")
                .longOpt("app")
                .hasArg()
                .build();

        Option optionOutput = Option.builder("o")
                .required(false)
                .desc("This parameter specifies the exported file path. If not specified, output will be directed to stdout.")
                .longOpt("output")
                .hasArg()
                .build();

        Option optionSpark = Option.builder(null)
                .required(false)
                .desc("Use Soot's SPARK for call graph (more precise but expensive)")
                .longOpt("spark")
                .hasArg(false)
                .build();

        Option optionIntra = Option.builder(null)
                .required(false)
                .desc("Run intra-procedural analysis (testing only)")
                .longOpt("intra")
                .hasArg(false)
                .build();

        Options options = new Options();
        options.addOption(optionApp);
        options.addOption(optionOutput);
        options.addOption(optionSpark);
        options.addOption(optionIntra);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            boolean use_spark = false;
            boolean run_intra = false;

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

            /* getting optional parameters */
            if (commandLine.hasOption('o')) {
                /* getting option o */
                String filePath = commandLine.getOptionValue('o');
                PrintStream fileOut = new PrintStream(new FileOutputStream(filePath, true));
                System.setOut(fileOut);
            }
            if (commandLine.hasOption("spark")) {
                /* getting option spark */
                use_spark = true;
            }
            if (commandLine.hasOption("intra")) {
                /* getting option intra */
                run_intra = true;
            }

            run(considered, use_spark, run_intra);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            new HelpFormatter().printHelp("ccc", options);
        }
    }

    private static void run(String[][] considered, boolean use_spark, boolean run_intra) throws IOException {
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
        if (run_intra) {
            driver.runIntraTaintAnalysis(srcPaths, classPaths);
        } else {
            driver.runInterTaintAnalysis(srcPaths, classPaths, use_spark);
        }
    }

}
