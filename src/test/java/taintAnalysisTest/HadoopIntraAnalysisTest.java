package taintAnalysisTest;

import configInterface.ConfigInterface;
import configInterface.HadoopInterface;
import org.junit.Test;
import org.junit.Assert;
import soot.SootMethod;
import soot.jimple.Stmt;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.sourceSinkManager.SourceSinkManager;

import java.util.*;

public class HadoopIntraAnalysisTest extends TaintAnalysisTest {
//
//    @Test
//    public void testIntraAnalysisHadoop() {
//        System.out.println("#### testing IntraAnalysis on Hadoop");
//        String hadoopJar = "app/hadoop-3.3.0/share/hadoop/common/hadoop-common-3.3.0.jar";
//        List<String> srcPaths = new ArrayList<>();
//        srcPaths.add(hadoopJar);
//        ConfigInterface configInterface = new HadoopInterface();
//        ISourceSinkManager sourceSinkManager = new SourceSinkManager(configInterface);
//        TaintAnalysisDriver driver = new TaintAnalysisDriver(sourceSinkManager);
//        List<List<Taint>> results = driver.runIntraTaintAnalysis(srcPaths, srcPaths).getSourceLists();
//        for (List<Taint> result : results) {
////            System.out.println("> result");
//            if (result.size() > 0) {
//                SootMethod method = result.get(0).getMethod();
//                String methodString = method.toString();
//                String declaringClass = method.getDeclaringClass().toString();
//                Stmt stmt = result.get(0).getStmt();
//                if (methodString.contains("setConf") && declaringClass.contains("ScriptBasedMapping$") ) {
//                    System.out.println("##### ScriptBasedMapping object using setConf method");
//                    Set<Taint> succ = result.get(0).getSuccessors();
//                    Assert.assertEquals(succ.size(), 1);
//                    for (Taint s : succ) {
//                        System.out.println(s.getStmt().toString());
//                        String fieldName = s.getStmt().getFieldRef().getField().getName();
//                        System.out.println(fieldName);
//                        Assert.assertEquals(fieldName, "scriptName");
//                    }
//                } else if (methodString.contains("start")) {
//                    // TODO: client, this.curator should also be tainted
//                    System.out.println(methodString);
//                    Assert.assertEquals(declaringClass, "org.apache.hadoop.util.curator.ZKCuratorManager");
//                } else if (methodString.contains("loadSSLConfiguration")) {
//                    System.out.println("##### HttpServer2 object using loadSSLConfiguration method");
//                    Assert.assertEquals(declaringClass.contains("HttpServer2"), true);
//                    Set<Taint> succ = result.get(0).getSuccessors();
//                    Assert.assertEquals(succ.size(), 1);
//                    Set<String> httpServerFieldNameSet = new HashSet<>(Arrays.asList("needsClientAuth", "keyStore", "keyStorePassword",
//                            "keyStoreType", "keyPassword", "trustStore", "trustStorePassword", "trustStoreType", "excludeCiphers"));
//                    for (Taint s : succ) {
//                        String fieldName = s.getStmt().getFieldRef().getField().getName();
//                        Assert.assertEquals(httpServerFieldNameSet.contains(fieldName), true);
//                    }
//                }
//            }
//        }
//    }
}
