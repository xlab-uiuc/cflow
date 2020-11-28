public class SimpleIntraAnalysisTest {

    int i1;
    int i2;

    public void run() {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test10();
    }

    private int source() {
        return 7;
    }

    private void callee(Book b1, Book b2, int v) {
        if (v > 1) {
            v = source(); // v shouldn't be
            b2.a = source();
            i1 = b2.a;
            return;
        } else {
            v = source();
            b1.a = source();
            i1 = b1.a;
            return;
        }
    }

    public Book callee2(Book b1, Book b2, int v) {
        Book book = new Book();
        if (v > 1) {
            b2.a = source();
            book.a = b2.a;
        } else {
            b1.a = source();
            book.a = b1.a;
        }

        return book;
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
        // Loops
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
        a = 20; // a's taint is killed
        int b = a; // b should not be tainted
        System.out.println(b);
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

    public void test8() {
        // Multiple sources
        int a = source();
        int b = source();
        System.out.println(a);
        System.out.println(b);
    }

    public void test10() {
        // Test visitReturn and visitReturnVoid
        Book book1 = new Book();
        Book book2 = new Book();
        int dummy = 0;
        callee(book1, book2, dummy);
        Book book3 = callee2(book1, book2, dummy);
        i1 = book1.a;
        System.out.println(i1);
    }
}
