package taintAnalysisTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import soot.SootMethod;
import soot.jimple.parser.node.TAbstract;
import soot.util.backend.SootASMClassWriter;
import taintAnalysis.IntraAnalysisTransformer;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SimpleIntraAnalaysisTest extends TaintAnalysisTest {

    @Test
    public void testIntraAnalysisSimpleTest() throws IOException {
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        IntraAnalysisTransformer transformer = driver.run();
        List<List<Taint>> results = transformer.getSources();
        for (List<Taint> result : results) {
            if (result.size() > 0) {
                String method = result.get(0).getMethod().toString();
                if (method.contains("test4")) {
                    Assert.assertEquals(0, result.size());
                } else if (method.contains("test8")) {
                    Assert.assertEquals(2, result.size());
                } else {
                    Assert.assertEquals(1, result.size());
                }
            }
        }

        Map<SootMethod, List<Set<Taint>>> methodSummary = transformer.getMethodSummary();
        for (SootMethod method : methodSummary.keySet()) {
            if (method.toString().contains("<Test: void callee(Book,Book)>")) {
                // test if taint on parameter is recorded
                List<Set<Taint>> taintList = methodSummary.get(method);
                Assert.assertEquals(taintList.size(), 3); // callee function records for (@parameter0, @parameter1, @this)
                Assert.assertEquals(taintList.get(1).size() > 0, true); // book 2 (@parameter1) shoud be tainted
                for (Taint t : taintList.get(1)) {
                    System.out.println(t.toString());
                }
                Assert.assertEquals(taintList.get(2).size(), 0); // @this is not tainted
            }
        }
    }


}
