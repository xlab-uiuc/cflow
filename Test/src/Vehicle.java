public class Vehicle {

    int a;

    Vehicle() {
        a = source();
    }

    Vehicle(int a_) {
        a = a_;
    }

    int source() {
        return 7;
    }

    public void dynamicBinding1(Vehicle v) {
        a = v.a;
    }

    public void dynamicBinding2(Vehicle v) {
        a = source() + 1;
    }

}