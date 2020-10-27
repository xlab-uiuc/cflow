public class Cat implements Animal {

    int a;
    String b;

    Cat() {
        a = 10;
    }

    int source() {
        return 1;
    }

    @Override
    public void sleep() {
        a = source();
    }

    @Override
    public void eat() {
        System.out.println("The cat eats.");
    }

    @Override
    public void makeSound() {
        System.out.println("The cat meows.");
    }

    @Override
    public void dynamicBinding(Animal cat) {
        Cat c = (Cat) cat;
        a = c.a + source();
    }
}