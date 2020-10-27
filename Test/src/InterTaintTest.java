public class InterTaintTest {

    private int source() {
        return 7;
    }

    public InterTaintTest() {

    }

    public void testInheritance() {
        Vehicle v1 = new Car();
        Vehicle v2 = new Car();
        v1.dynamicBinding1(v2);
        v1.dynamicBinding2(v2);
    }

    public void testInterface() {
        Cat cat1 = new Cat();
        Cat cat2 = new Cat();
        cat1.dynamicBinding(cat2);
    }
}