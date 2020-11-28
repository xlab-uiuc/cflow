public class ConditionalFlowTest {

    private int source() {
        return 7;
    }

    public void run() {
        test();
    }

    private void test() {
        int s = source();
        if (s > 10) {
            System.out.println(s);
        } else {
            System.out.println(s + 1);
        }
        int c = s + 10;
        System.out.println(c);
    }

}
