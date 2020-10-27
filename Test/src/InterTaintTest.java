public class InterTaintTest {

    private int source() {
        return 7;
    }

    public InterTaintTest() {

    }

    public void testInheritance() {
        Vehicle v = new Car();
    }
}