import java.util.ArrayList;
import java.util.List;

public class TaintWrapperTest {

    private int source(int base) { return 7 + base; }

    private String source() {
        return "7";
    }

    public void run() {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
    }

    // Taint both
    private void test1() {
        String s = source();
        StringBuilder str = new StringBuilder(s);
        str.append("lucky");
        System.out.println(str);
    }

    // Taint base
    private void test2() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        String s = lst.get(0);
        System.out.println(s);
    }

    // Taint return
    private void test3() {
        int a = source(7);
        int max = Math.max(a, 5);
        System.out.println(max);
    }

    // Exclude
    private void test4() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        int len = lst.size();
        System.out.println(len);
    }

    // Kill Taint
    private void test5() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.clear();
        lst.add("1");
        String s = lst.get(0);
        System.out.println(s);
    }

    // Not registered
    private void test6() {
        // TODO
    }

}
