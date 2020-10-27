public class MainClass {

    public static void main(String[] args) {
        // Test basics of intra-procedural analysis
        Test main = new Test();
        main.test1();
        main.test2();
        main.test3();
        main.test4();
        main.test5();
        main.test6();
        main.test7();
        main.test8();
        main.test10();

        // Test basics of inter-procedural analysis
        SimpleInterAnalysisTest simpleInterAnalysisTest = new SimpleInterAnalysisTest();
        simpleInterAnalysisTest.run();

        // Test behaviors related to dynamic binding
        InterTaintTest main2 = new InterTaintTest();
        main2.testInheritance();
        main2.testInterface();

        // Test nested field access
        NestedFieldTest nestedFieldTest = new NestedFieldTest();
        nestedFieldTest.run();

        // Test recursive call
        RecursiveCallTest recursiveCallTest = new RecursiveCallTest();
//        recursiveCallTest.run();
    }

}
