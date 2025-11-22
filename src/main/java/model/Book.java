package model;

import org.bson.Document;

public abstract class Book {
    private final String isbn;
    private final String title;
    private final String author;

    protected Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }

    public Document toDocument() {
        Document d = new Document("isbn", isbn)
                .append("title", title)
                .append("author", author);
        return d;
    }

    public static Book fromDocument(Document doc) {
        if (doc == null) return null;
        String type = doc.getString("type");
        if ("ebook".equalsIgnoreCase(type)) {
            String isbn = doc.getString("isbn");
            String title = doc.getString("title");
            String author = doc.getString("author");
            String downloadUrl = doc.getString("downloadUrl");
            return new EBook(isbn, title, author, downloadUrl);
        } else {
            // printed book
            String isbn = doc.getString("isbn");
            String title = doc.getString("title");
            String author = doc.getString("author");
            Integer copies = doc.getInteger("copiesAvailable", 0);
            Boolean isAvailable = doc.getBoolean("isAvailable", copies != null && copies > 0);
            return new PrintedBook(isbn, title, author, copies == null ? 0 : copies, isAvailable);
        }
    }
}