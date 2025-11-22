package model;

import org.bson.Document;

public class Admin extends User {
    public Admin(String id, String username, String password) {
        super(id, username, password, "admin");
    }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        // no extra fields for Admin now
        return d;
    }
}