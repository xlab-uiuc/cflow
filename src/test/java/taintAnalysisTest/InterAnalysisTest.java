package taintAnalysisTest;

import org.junit.Assert;
import org.junit.Test;
import soot.SootMethod;
import taintAnalysis.*;
import taintAnalysis.sourceSinkManager.ISourceSinkManager;
import taintAnalysis.sourceSinkManager.SourceSinkManager;
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
        ISourceSinkManager sourceSinkManager = new SourceSinkManager(Config.getInterface(cfg));
        TaintAnalysisDriver driver = new TaintAnalysisDriver(sourceSinkManager);
        InterAnalysisTransformer transformer = driver.runInterTaintAnalysis(srcPaths, classPaths, false);
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
            } else if (method.toString().contains("<Car: void dynamicBinding1(Vehicle)>")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(3, taintList.size()); // @this, @return, @parameter0
                Assert.assertEquals(1, taintList.get(0).size()); // @this.a should be tainted
            } else if (method.toString().contains("<Car: void dynamicBinding2(Vehicle)>")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(3, taintList.size()); // @this, @return, @parameter0
                Assert.assertEquals(0, taintList.get(0).size()); // @this.a should not be tainted
            }
            // test dynamic binding in interface
            else if (method.toString().contains("<Cat: void dynamicBinding(Animal)>")) {
                List<Set<Taint>> taintList = methodSummary.get(method).get(Taint.getEmptyTaint());
                Assert.assertEquals(3, taintList.size()); // @this, @return, @parameter0
                Assert.assertEquals(1, taintList.get(0).size());
                for (Set<Taint> s : taintList) {
                    for (Taint t : s) {
                        System.out.println("[Cat.dynamicBinding(Animal)] > " + t.toString());
                    }
                }
            }
        }

        List<Taint> sources = transformer.getSources();
        for (Taint t : sources) {
            System.out.println(t.toString());
        }
    }
}
