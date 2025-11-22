package model;

import org.bson.Document;

public abstract class User {
    private final String id;
    private final String username;
    private final String password;
    private final String role; // "admin", "librarian", "reader"

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    public Document toDocument() {
        Document doc = new Document("id", id)
                .append("username", username)
                .append("password", password)
                .append("role", role);
        // subclasses may add more fields
        return doc;
    }

    public static User fromDocument(Document doc) {
        if (doc == null) return null;
        String role = doc.getString("role");
        String id = doc.getString("id");
        String username = doc.getString("username");
        String password = doc.getString("password");

        if ("admin".equalsIgnoreCase(role)) {
            return new Admin(id, username, password);
        } else if ("librarian".equalsIgnoreCase(role)) {
            return new Librarian(id, username, password);
        } else if ("student".equalsIgnoreCase(role)) {
            // Student documents may include a studentNumber field
            String studentNumber = doc.getString("studentNumber");
            return new Student(id, username, password, studentNumber);
        } else {
            // default to Reader
            return new Reader(id, username, password);
        }
    }
}