package model;

public class PhysicalBook extends Book {
    private final int copiesAvailable;

    public PhysicalBook(String isbn, String title, String author, int copiesAvailable) {
        super(isbn, title, author);
        this.copiesAvailable = copiesAvailable;
    }

    public int getCopiesAvailable() { return copiesAvailable; }

    @Override
    public String toString() {
        return "PhysicalBook{" + "isbn='" + getIsbn() + "', title='" + getTitle() + "', copiesAvailable=" + copiesAvailable + "}";
    }
}