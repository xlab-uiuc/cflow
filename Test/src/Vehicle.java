public class Vehicle {

    int a;

    Vehicle() {
        a = source();
    }

    Vehicle(int a_) {
        a = a_;
    }

    private int source() {
        return 7;
    }


}