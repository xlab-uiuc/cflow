public class Test {

    private class Inner {
        int a = 0;
        int b = 0;
    }

    int i1;
    int i2;

    public Test() {

    }

    private int source() {
        return 7;
    }

    public void test1() {
        // Trivial case
        int a = source();
        int b = a + 10;
        System.out.println(b);
    }

    public void test2() {
        // If Branches
        int a = source();
        int b;
        if (a > 10) {
            b = 7;
        } else {
            b = a + 10;
        }
        int c = b + 20; // c should be tainted
        System.out.println(c);
    }

    public void test3() {
        // Loops -- TODO: Infinite Loop!
        int a = source();
        int b = 0;
        for (int i = 0; i < 10; i++) {
            b = a + 10;
            b = b + 15;
        }
        int c = b + 20;
        System.out.println(c);
    }

    public void test4() {
        // Kill Taint
        int a = source();
        System.out.println(a);
        a = 20; // a's taint is killed
        int b = a; // b should not be tainted
        System.out.println(a);
    }

    public void test5() {
        // Fields 1
        i1 = source();
        i2 = i1 + 10;
        System.out.println(i2);
    }

    public void test6() {
        // Fields 2
        Book book1 = new Book();
        Book book2 = new Book();
        book1.a = source();
        book2 = book1;
        i1 = book2.a; // i1 should be tainted
        System.out.println(i1);
    }

    public void test7() {
        // Fields 3
        Book book1 = new Book();
        Book book2 = new Book();
        book1.a = source();
        book1 = book2;
        i1 = book1.a; // i1 should not be tainted
        System.out.println(i1);
    }
}
