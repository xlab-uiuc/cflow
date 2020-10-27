public class MainClass {

    public static void main(String[] args) {
        Test main = new Test();
        main.test1();
        main.test2();
        main.test3();
        main.test4();
        main.test5();
        main.test6();
        main.test7();
        main.test8();
        int c = main.test9();
        System.out.println(c);
        main.test10();

        // Inter taint analysis test
        InterTaintTest main2 = new InterTaintTest();
        main2.testInheritance();
    }

}
