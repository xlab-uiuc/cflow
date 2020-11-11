public class ComplexContextSensitivityTest {

    private int source() {
        return 7;
    }

    public void run() {
        int s = source();
        A1(s);
        A2(s);
        D1(s);
        D2(s);
    }

    private void A1(int s) {
        int c = B1(s);
        System.out.println(c);
    }

    private void A2(int s) {
        int c = B1(s);
        int b = c + 10;
        System.out.println(c);
    }

    private int B1(int s) {
        int c = C(s);
        System.out.println(c);
        return c;
    }

    private int B2(int s) {
        int c = C(s);
        System.out.println(c);
        return c;
    }

    private void D1(int s) {
        int c = B2(s);
        System.out.println(c);
    }

    private void D2(int s) {
        int c = B2(s);
        System.out.println(c);
    }

    private int C(int i) {
        int ret = i + 10;
        return ret;
    }
}
