public class Car extends Vehicle {

    int b;

    Car() {
        super();
        b = 1;
    }

    @Override
    public void dynamicBinding1(Vehicle v) {
        a = v.a + source();
    }

    @Override
    public void dynamicBinding2(Vehicle v) {
        a = 2;
    }
}
