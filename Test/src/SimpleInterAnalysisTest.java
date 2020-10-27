public class SimpleInterAnalysisTest {

    int i1;
    int i2;

    public void run() {
        // Test tainting base
        test1();

        // Test tainting return value
        int ret = test2();
        System.out.println(ret);

        // Test tainting parameters
        int a = source();
        int b = 5;
        Book book = new Book();
        test3(a, b, book);
    }

    private int source() {
        return 7;
    }

    public void test1() {
        i1 = source();
        i2 = i1 + 10;
        System.out.println(i2);
    }

    public int test2() {
        int a = source();
        int b = a + 10;
        return b;
    }

    private void test3(int a, int b, Book book) {
        book.a = source();      // taint shall be retained after exiting the method
        a = 5;                  // shall not kill killed the taint on a outside the method
        b = source();           // taint shall be killed after exiting the method
        System.out.println(a);
        System.out.println(b);
    }

}
