package taintAnalysisTest;

import org.junit.Assert;
import org.junit.Test;
import soot.SootMethod;
import taintAnalysis.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterAnalysisTest extends TaintAnalysisTest {
    @Test
    public void testInterTaintAnalysis() {
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        InterAnalysisTransformer transformer = driver.runInterTaintAnalysis();
        Map<SootMethod, Map<Taint, List<Set<Taint>>>> methodSummary = transformer.getMethodSummary();
        for (SootMethod method : methodSummary.keySet()) {
            if (method.toString().contains("<Vehicle: void <init>()>")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(2, taintList.size());
                Assert.assertEquals(1, taintList.get(0).size()); // @this should be tainted
            }
            if (method.toString().contains("<Car: void <init>()>")) { // subclass of Vehicle
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(2, taintList.size());
                Assert.assertEquals(1, taintList.get(0).size()); // @this should be tainted
            }
        }
    }
}
