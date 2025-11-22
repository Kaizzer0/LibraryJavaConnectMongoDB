package model;

import org.bson.Document;

public class Reader extends User {
    public Reader(String id, String username, String password) {
        super(id, username, password, "reader");
    }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        return d;
    }
}