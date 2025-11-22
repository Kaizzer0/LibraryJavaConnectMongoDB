package model;

import org.bson.Document;

public class Librarian extends User {
    public Librarian(String id, String username, String password) {
        super(id, username, password, "librarian");
    }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        return d;
    }
}