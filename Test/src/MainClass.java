public class MainClass {

    public static void main(String[] args) {
        // Test basics of intra-procedural analysis
        SimpleIntraAnalysisTest simpleIntraAnalysisTest = new SimpleIntraAnalysisTest();
        simpleIntraAnalysisTest.run();

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
        recursiveCallTest.run();

        // Test context sensitive path building
        ContextSensitivityTest contextSensitivityTest = new ContextSensitivityTest();
        contextSensitivityTest.run();

        ComplexContextSensitivityTest complexContextSensitivityTest = new ComplexContextSensitivityTest();
        complexContextSensitivityTest.run();

        // Test taint wrapper for common external API calls
        TaintWrapperTest taintWrapperTest = new TaintWrapperTest();
        taintWrapperTest.run();

        ConditionalFlowTest conditionalFlowTest = new ConditionalFlowTest();
        conditionalFlowTest.run();
    }

}
