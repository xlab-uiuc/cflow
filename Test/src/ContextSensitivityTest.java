public class ContextSensitivityTest {

    private int source() {
        return 7;
    }

    public void run() {
        int s = source();
        A(s);
        B(s);
        D(s);
    }

    private void A(int s) {
        int c = C(s);
        System.out.println(c);
    }

    private void B(int s) {
        int c = C(s);
        System.out.println(c);
    }

    private void D(int s) {
        int c = C(s);
        System.out.println(c);
    }

    private int C(int i) {
        int ret = i + 10;
        return ret;
    }

}
