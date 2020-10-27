public class RecursiveCallTest {

    private int source() {
        return 7;
    }

    public void run() {
        int res = fac(source());
        System.out.println(res);
    }

    private int fac(int i) {
        if (i == 0) {
            return 1;
        }
        return i * fac(i - 1);
    }

}
