package service;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import model.Admin;
import model.Librarian;
import model.Reader;
import model.Student;
import model.User;
import model.Book;

public class LibraryManagementSystem {
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> books;
    private final MongoCollection<Document> transactions;

    public LibraryManagementSystem(MongoDatabase db) {
        this.users = db.getCollection("users");
        this.books = db.getCollection("products");  // Changed from "books" to "products"
        this.transactions = db.getCollection("transactions");
    }

    // New helper required by seed code and controller
    public User findUserByUsername(String username) {
        if (username == null) return null;
        Document d = users.find(eq("username", username)).first();
        return docToUser(d);
    }

    public User login(String username, String password) {
        Document d = users.find(eq("username", username)).first();
        if (d == null) return null;
        String pw = d.getString("password");
        if (pw == null || !pw.equals(password)) return null;
        return docToUser(d);
    }

    private User docToUser(Document d) {
        if (d == null) return null;
        String id = d.getString("id");
        String username = d.getString("username");
        String password = d.getString("password");
        String role = d.getString("role");
        if (role == null) role = "reader";
        switch (role.toLowerCase()) {
            case "admin": return new Admin(id == null ? "u-"+username : id, username, password);
            case "librarian": return new Librarian(id == null ? "u-"+username : id, username, password);
            case "student": return new Student(id == null ? "u-"+username : id, username, password, d.getString("studentNumber"));
            default: return new Reader(id == null ? "u-"+username : id, username, password);
        }
    }

    public void addUser(User user) {
        if (user == null) throw new IllegalArgumentException("user==null");
        Document d = new Document()
                .append("id", user.getId())
                .append("username", user.getUsername())
                .append("password", user.getPassword())
                .append("role", user.getRole());
        if (user instanceof Student) d.append("studentNumber", ((Student) user).getStudentNumber());
        users.insertOne(d);
    }

    public void addStudent(String id, String username, String password, String studentNumber) {
        Student s = new Student(id, username, password, studentNumber);
        addUser(s);
    }

    public boolean deleteUserByUsername(String username) {
        DeleteResult res = users.deleteOne(eq("username", username));
        return res.getDeletedCount() > 0;
    }

    public List<User> listAllUsers() {
        List<User> out = new ArrayList<>();
        for (Document d : users.find()) {
            User u = docToUser(d);
            if (u != null) out.add(u);
        }
        return out;
    }

    public void addBook(Book b) {
        if (b == null) throw new IllegalArgumentException("book==null");
        Document d = b.toDocument();
        String now = Instant.now().toString();
        d.putIfAbsent("createdDate", now);
        d.put("lastUpdated", now);
        if (!d.containsKey("status") || d.getString("status") == null) d.put("status", "Available");
        d.remove("price");
        books.insertOne(d);
    }

    public boolean updateBookByIsbn(String isbn, Book updated) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String now = Instant.now().toString();
        Document u = updated.toDocument();
        u.put("lastUpdated", now);
        u.remove("price");
        books.updateOne(eq("isbn", isbn),
                combine(
                        set("name", u.getString("name")),
                        set("category", u.getString("category")),
                        set("status", u.getString("status")),
                        set("isbn", u.getString("isbn")),
                        set("lastUpdated", u.getString("lastUpdated")),
                        set("borrowedBy", u.getString("borrowedBy"))
                ));
        return true;
    }

    public boolean borrowBook(String title, String username) {
        if (title == null || title.trim().isEmpty()) return false;
        Document doc = books.find(and(regex("name", "^" + java.util.regex.Pattern.quote(title) + "$", "i"), eq("status", "Available"))).first();
        if (doc == null) {
            doc = books.find(and(regex("name", title, "i"), eq("status", "Available"))).first();
        }
        if (doc == null) return false;
        String now = Instant.now().toString();
        books.updateOne(eq("_id", doc.get("_id")),
                combine(set("status", "Borrowed"), set("borrowedBy", username), set("lastUpdated", now)));
        Document tx = new Document("action", "borrow")
                .append("title", doc.getString("name"))
                .append("isbn", doc.getString("isbn"))
                .append("username", username)
                .append("timestamp", now);
        transactions.insertOne(tx);
        return true;
    }

    public boolean returnBook(String title, String username) {
        if (title == null || title.trim().isEmpty()) return false;
        Document doc = books.find(and(regex("name", "^" + java.util.regex.Pattern.quote(title) + "$", "i"), eq("status", "Borrowed"), eq("borrowedBy", username))).first();
        if (doc == null) {
            doc = books.find(and(regex("name", title, "i"), eq("status", "Borrowed"), eq("borrowedBy", username))).first();
        }
        if (doc == null) return false;
        String now = Instant.now().toString();
        books.updateOne(eq("_id", doc.get("_id")),
                combine(set("status", "Available"), set("borrowedBy", null), set("lastUpdated", now)));
        Document tx = new Document("action", "return")
                .append("title", doc.getString("name"))
                .append("isbn", doc.getString("isbn"))
                .append("username", username)
                .append("timestamp", now);
        transactions.insertOne(tx);
        return true;
    }

    public List<Book> listAllBooks() {
        List<Book> out = new ArrayList<>();
        for (Document d : books.find()) {
            Book b = Book.fromDocument(d);
            if (b != null) out.add(b);
        }
        return out;
    }
}