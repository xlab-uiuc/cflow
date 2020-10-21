package taintAnalysisTest;

import org.junit.Test;
import org.junit.Assert;
import soot.SootMethod;
import taintAnalysis.IntraAnalysisTransformer;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SimpleIntraAnalysisTest extends TaintAnalysisTest {

    @Test
    public void testIntraAnalysisSimpleTest() {
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        IntraAnalysisTransformer transformer = driver.runIntraTaintAnalysis();
        List<List<Taint>> results = transformer.getSourceLists();
        for (List<Taint> result : results) {
            if (result.size() > 0) {
                String method = result.get(0).getMethod().toString();
                if (method.contains("test4")) {
                    Assert.assertEquals(0, result.size());
                } else if (method.contains("test8")) {
                    Assert.assertEquals(2, result.size());
                } else if (!method.contains("callee")) {
                    Assert.assertEquals(1, result.size());
                }
            }
        }

        Map<SootMethod, Map<Set<Taint>, List<Set<Taint>>>> methodSummary = transformer.getMethodSummary();
        for (SootMethod method : methodSummary.keySet()) {
            if (method.toString().contains("<Test: void callee(Book,Book,int)>")) {
                System.out.println("[TEST]> testing <Test: void callee(Book,Book,int)>");
                // test if taint on parameter is recorded
                List<Set<Taint>> taintList = methodSummary.get(method).get(new HashSet<>());
                Assert.assertEquals(4, taintList.size()); // callee function records for (@parameter0, @parameter1, @parameter2, @this)
                Assert.assertEquals(true, taintList.get(0).size() > 0); // book1 (@parameter0) should be tainted
                for (Taint t : taintList.get(0)) {
                    System.out.println(t.toString());
                }
                Assert.assertEquals(true, taintList.get(1).size() > 0); // book2 (@parameter1) should be tainted
                for (Taint t : taintList.get(1)) {
                    System.out.println(t.toString());
                }
                Assert.assertEquals(true, taintList.get(3).size() > 0); // @this should be tainted as well
                for (Taint t : taintList.get(3)) {
                    System.out.println(t.toString());
                }
            } else if (method.toString().contains("<Test: Book callee2(Book,Book,int)>")) {
                System.out.println("[TEST]> testing <Test: Book callee2(Book,Book,int)>");
                List<Set<Taint>> taintList = methodSummary.get(method).get(new HashSet<>());
                Assert.assertEquals(5, taintList.size()); // callee function records for (@parameter0, @parameter1, @parameter2, @this, return value)
                Assert.assertEquals(true, taintList.get(4).size() > 0); // return value should be tainted
                for (Taint t : taintList.get(4)) {
                    System.out.println(t.toString());
                }
            }
        }
    }

}
