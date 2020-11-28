public class NestedFieldTest {

    Book book;

    private int source() {
        return 7;
    }

    public void run() {
        book = new Book();
        book.a = source();
    }

}
