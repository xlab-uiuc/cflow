//import acai.Acai;
//import acai.configInterface.ConfigInterface;
//import acai.flowdroid.AcaiInfoflowResultHandler;
//import acai.utility.AcaiConfig;
//import acai.utility.FlowComparator;
import checking.*;
import configInterface.ConfigInterface;
import org.apache.commons.cli.*;
import taintAnalysis.TaintAnalysisDriver;
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
            /* getting option x */
            String xmlPath = commandLine.getOptionValue('x');
            File folder = new File(xmlPath);
            File[] xmlFiles = folder.listFiles();
            String[] fileNames = new String[xmlFiles.length];
            for (int i = 0; i < xmlFiles.length; i++)
                fileNames[i] = xmlFiles[i].getAbsolutePath();

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

    private static void run(String[][] considered, boolean intra) {
        List<String> srcPaths = new LinkedList<>();
        List<String> classPaths = new LinkedList<>();
        ConfigInterface configInterface = null;

        for (String[] cfg : considered) {
            srcPaths.addAll(Config.getSourcePaths(cfg));
            classPaths.addAll(Config.getClassPaths(cfg));
            configInterface = Config.getInterface(cfg);
        }

        // Run taint analysis
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        if (intra) {
            driver.runIntraTaintAnalysis(srcPaths, classPaths, configInterface);
        } else {
            driver.runInterTaintAnalysis(srcPaths, classPaths, configInterface);
        }

//         // Run checking
//        runChecking(configInterface, results, considered);
    }
//
//    private static void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered) throws IOException, SAXException, ParserConfigurationException {
//        //CheckPass dataTypeChk = new DataTypeChk();
//        //dataTypeChk.runChecking(configInterface, results, considered);
//
//        //CheckPass defaultValueChk = new DefaultValueChk();
//        //defaultValueChk.runChecking(configInterface, results, considered);
//
//        CheckPass casePass = new CaseSensitivityChk();
//        casePass.runChecking(configInterface, results, considered);
//
//        //CheckPass unusedParamPass = new UnusedParamChk();
//        //unusedParamPass.runChecking(configInterface, results, considered);
//    }
//
}
