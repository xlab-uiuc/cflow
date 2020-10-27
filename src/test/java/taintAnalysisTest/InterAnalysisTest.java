package taintAnalysisTest;

import configInterface.ConfigInterface;
import org.junit.Assert;
import org.junit.Test;
import soot.SootMethod;
import taintAnalysis.*;
import utility.Config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterAnalysisTest extends TaintAnalysisTest {
    @Test
    public void testInterTaintAnalysis() {
        String[] cfg = Config.getCfg("test");
        List<String> srcPaths = Config.getSourcePaths(cfg);
        List<String> classPaths = Config.getClassPaths(cfg);
        ConfigInterface configInterface = Config.getInterface(cfg);
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        IntraAnalysisTransformer transformer = driver.runIntraTaintAnalysis(srcPaths, classPaths, configInterface);
        Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary = transformer.getMethodSummary();

        for (SootMethod method : methodSummary.keySet()) {
            // test dynamic binding in inheritance
            if (method.toString().contains("<Vehicle: void <init>()>")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(2, taintList.size());
                Assert.assertEquals(1, taintList.get(0).size()); // @this should be tainted
            } else if (method.toString().contains("<Car: void <init>()>")) { // subclass of Vehicle
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(2, taintList.size());
                Assert.assertEquals(1, taintList.get(0).size()); // @this should be tainted
            } else if (method.toString().contains("Car: void dynamicBinding1(Vehicle)")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(3, taintList.size()); // @this, @return, @parameter0
                Assert.assertEquals(1, taintList.get(0).size()); // @this.a should be tainted
            } else if (method.toString().contains("Car: void dynamicBinding2(Vehicle)")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(3, taintList.size()); // @this, @return, @parameter0
                Assert.assertEquals(0, taintList.get(0).size()); // @this.a should not be tainted
            }
            // TODO: test dynamic binding in interface

        }
    }
}
