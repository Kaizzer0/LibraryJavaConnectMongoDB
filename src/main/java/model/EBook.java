package model;

import org.bson.Document;

public class EBook extends Book {
    private final String downloadUrl;

    public EBook(String isbn, String title, String author, String downloadUrl) {
        super(isbn, title, author);
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl() { return downloadUrl; }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        d.append("type", "ebook")
         .append("downloadUrl", downloadUrl)
         .append("isAvailable", true); // ebooks are always available to "access"
        return d;
    }

    @Override
    public String toString() {
        return "EBook{" + "isbn='" + getIsbn() + "', title='" + getTitle() + "', downloadUrl='" + downloadUrl + "'}";
    }
}