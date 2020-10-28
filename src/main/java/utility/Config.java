package utility;

import assertion.Assert;
import configInterface.ConfigInterface;
import configInterface.HadoopInterface;
import configInterface.SparkInterface;
import configInterface.TestInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Config {

    public static final String TEST_PATH = "Test/out/artifacts/Test_jar/Test.jar";
    public static final String HADOOP_PATH = "app/hadoop-3.3.0/";
    public static final String HBASE_PATH = "app/hbase-2.3.1/";
    public static final String ALLUXIO_PATH = "app/alluxio-1.8.0/";
    public static final String ZOOKEEPER_PATH = "app/apache-zookeeper-3.5.6-bin/";
    public static final String SPARK_PATH = "app/spark-2.4.6-bin-hadoop2.7/";
    public static final String[] SUPPORTED = {"test","hdfs","mapreduce","yarn","hadoop_common","hadoop_tools",
            "hbase","alluxio","zookeeper","spark"};

    public static final String[][] CONFIGS = {
            {       "test",
                    "",
                    "",
            },
            {
                    "hadoop",
                    "hdfs",
                    HADOOP_PATH + "share/hadoop/hdfs/",
            },
            {
                    "hadoop",
                    "mapreduce",
                    HADOOP_PATH + "share/hadoop/mapreduce/"
            },
            {
                    "hadoop",
                    "yarn",
                    HADOOP_PATH + "share/hadoop/yarn/"
            },
            {
                    "hadoop",
                    "common",
                    HADOOP_PATH + "share/hadoop/common/"
            },

            {
                    "hadoop",
                    "tools",
                    HADOOP_PATH + "share/hadoop/tools/lib/"
            },
            {
                    "hbase",
                    "",
                    HBASE_PATH + "lib/"
            },
            {
                    "alluxio",
                    "",
                    ALLUXIO_PATH + "lib/"
            },
            {
                    "zookeeper",
                    "",
                    ZOOKEEPER_PATH + "lib/"
            },
            {
                    "spark",
                    "",
                    SPARK_PATH + "jars/"
            },
    };

    public static String[] getCfg(String app) throws IllegalArgumentException {
        System.out.println(app);
        int i = 0;
        for (; i < SUPPORTED.length; i++) {
            if (SUPPORTED[i].equals(app)) {
                return CONFIGS[i];
            }
        }
        if (i == SUPPORTED.length) {
            throw new IllegalArgumentException(app + " not found in supported application");
        }
        return null;
    }

    public static ConfigInterface getInterface(String[] cfg){
//        if (cfg[0].contains("alluxio")) {
//            return new AlluxioInterface();
//        }
//        if (cfg[0].contains("zookeeper")) {
//            return new ZooKeeperInterface();
//        }
        if (cfg[0].contains("test")) {
            return new TestInterface();
        }
        if (cfg[0].contains("spark")) {
            return new SparkInterface();
        }
        return new HadoopInterface();
    }

    public static List<String> getClassPaths(String[] cfg) {
        String clsRoot;
        List<String> classPaths = new LinkedList<>();

        if (cfg[0].compareTo("test") == 0) {
            classPaths.add(Config.TEST_PATH);
        }

        if (cfg[0].compareTo("hadoop") == 0) {
            clsRoot = Config.HADOOP_PATH;
            if (cfg[1].contains("hdfs")) {
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/hdfs/lib/"));
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/hdfs/"));
            } else if (cfg[1].contains("yarn")) {
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/yarn/lib/"));
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/yarn/"));
            } else if (cfg[1].contains("mapreduce")) {
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/mapreduce/"));
            } else if (cfg[1].contains("tools")) {
                classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/tools/lib/"));
            } else if (cfg[1].contains("common")) {

            } else {
                Assert.assertImpossible("UNRECOGNIZED COMPONENT: " + cfg[1]);
            }

            classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/common/lib/"));
            classPaths.addAll(getSootClassPaths(clsRoot + "share/hadoop/common/"));
        }

        if (cfg[0].compareTo("hbase") == 0) {
            clsRoot = Config.HBASE_PATH;
            classPaths.addAll(getSootClassPaths(clsRoot + "lib/"));
        }

        if (cfg[0].compareTo("alluxio") == 0) {
            clsRoot = Config.ALLUXIO_PATH;
            classPaths.addAll(getSootClassPaths(clsRoot + "lib/"));
        }

        if (cfg[0].compareTo("zookeeper") == 0){
            clsRoot = Config.ZOOKEEPER_PATH;
            classPaths.addAll(getSootClassPaths(clsRoot + "lib/"));
        }

        if (cfg[0].compareTo("spark") == 0){
            clsRoot = Config.SPARK_PATH;
            classPaths.addAll(getSootClassPaths(clsRoot + "jars/"));
        }

        return classPaths;
    }

    public static List<String> getSourcePaths(String[] cfg) {
        if (cfg[0].compareTo("test") == 0) {
            List<String> sourcePaths = new ArrayList<>();
            sourcePaths.add(Config.TEST_PATH);
            return sourcePaths;
        }

        if (cfg[0].compareTo("hadoop") == 0) {
            List<String> sourcePaths = new ArrayList<>();

            // include Hadoop Common for all Hadoop subcomponents
            String[] hadoopCommonCfg = CONFIGS[4];
            sourcePaths.addAll(getJars(hadoopCommonCfg[2]));

            if (cfg[1].contains("tools")) {
                sourcePaths.addAll(getJarsHadoopTools(cfg[2]));
            } else if (!cfg[1].contains("common")){
                sourcePaths.addAll(getJars(cfg[2]));
            }

            return sourcePaths;
        }

        if (cfg[0].compareTo("hbase") == 0) {
            return getJarsHbase(cfg[2]);
        }

        if (cfg[0].compareTo("alluxio") == 0) {
            return getJars(cfg[2]);
        }

        if (cfg[0].compareTo("zookeeper") == 0) {
            return getJars(cfg[2]);
        }

        if (cfg[0].compareTo("spark") == 0) {
            return getJarsSpark(cfg[2]);
        }

        return null;
    }

    public static List<String> getJarsHadoopTools(String dir) {
        List<String> jars = new LinkedList<String>();
        File sdir = new File(dir);
        Assert.assertTrue(sdir.isDirectory());

        for (File f : sdir.listFiles()) {
            if (f.getName().endsWith(".jar") &&
                    f.getName().startsWith("hadoop") &&
                    !f.getName().contains("test") &&
                    !f.getName().contains("example")) {
                jars.add(f.getAbsolutePath());
            }
        }
        return jars;
    }

    public static List<String> getJarsHbase(String dir) {
        List<String> jars = new LinkedList<String>();
        File sdir = new File(dir);
        Assert.assertTrue(sdir.isDirectory());

        for (File f : sdir.listFiles()) {
            if (f.getName().endsWith(".jar") &&
                    f.getName().startsWith("hbase") &&
                    !f.getName().contains("test") &&
                    !f.getName().contains("example")) {
                jars.add(f.getAbsolutePath());
            }
        }
        return jars;
    }

    public static List<String> getJarsSpark(String dir) {
        List<String> jars = new LinkedList<String>();
        File sdir = new File(dir);
        Assert.assertTrue(sdir.isDirectory());

        for (File f : sdir.listFiles()) {
            if (f.getName().endsWith(".jar") &&
                    f.getName().startsWith("spark") &&
                    !f.getName().contains("test") &&
                    !f.getName().contains("example")) {
                jars.add(f.getAbsolutePath());
            }
        }
        return jars;
    }

    public static List<String> getJars(String dir) {
        List<String> jars = new LinkedList<String>();
        File sdir = new File(dir);
        Assert.assertTrue(sdir.isDirectory());

        for (File f : sdir.listFiles()) {
            if (f.getName().endsWith(".jar") &&
                    !f.getName().contains("test") &&
                    !f.getName().contains("example")) {
                jars.add(f.getAbsolutePath());
            }
        }
        return jars;
    }

    public static List<String> getSootClassPaths(String dir) {
        List<String> scp = new LinkedList<>();
        File fdir = new File(dir);
        if (fdir.isDirectory() == false) {
            System.out.println("[FATAL] " + dir + " should be a directory path");
        }
        for (File f : fdir.listFiles()) {
            if (f.getName().endsWith(".jar") &&
                    !f.getName().contains("test") &&
                    !f.getName().contains("example")) {
                scp.add(f.getAbsolutePath());
            }
        }
        return scp;
    }

}
