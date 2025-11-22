package model;

import org.bson.Document;

public class PrintedBook extends Book {
    private final int copiesAvailable;
    private final boolean isAvailable;

    public PrintedBook(String isbn, String title, String author, int copiesAvailable, boolean isAvailable) {
        super(isbn, title, author);
        this.copiesAvailable = copiesAvailable;
        this.isAvailable = isAvailable;
    }

    public int getCopiesAvailable() { return copiesAvailable; }
    public boolean isAvailable() { return isAvailable; }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        d.append("type", "printed")
         .append("copiesAvailable", copiesAvailable)
         .append("isAvailable", isAvailable);
        return d;
    }

    @Override
    public String toString() {
        return "PrintedBook{isbn='" + getIsbn() + "', title='" + getTitle() + "', copiesAvailable=" + copiesAvailable + ", isAvailable=" + isAvailable + "}";
    }
}