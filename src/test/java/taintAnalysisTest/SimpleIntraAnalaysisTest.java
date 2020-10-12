package taintAnalysisTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import soot.SootMethod;
import soot.util.backend.SootASMClassWriter;
import taintAnalysis.Taint;
import taintAnalysis.TaintAnalysisDriver;

import java.io.*;
import java.util.List;


public class SimpleIntraAnalaysisTest {

    @Test
    public void testIntraAnalysis() throws IOException {
        TaintAnalysisDriver driver = new TaintAnalysisDriver();
        List<List<Taint>> results = driver.run();
        for (List<Taint> result : results) {
            if (result.size() > 0) {
                String method = result.get(0).getMethod().toString();
                System.out.println(method);
                if (method.contains("test4")) {
                    Assert.assertEquals(0, result.size());
                } else if (method.contains("test8")) {
                    Assert.assertEquals(2, result.size());
                } else {
                    Assert.assertEquals(1, result.size());
                }
            }
        }
    }

    private void dfs(Taint t, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);
        for (Taint succ : t.getSuccessors()) {
            dfs(succ, depth + 1);
        }
    }
}
