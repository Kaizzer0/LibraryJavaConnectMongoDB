package service;

import static com.mongodb.client.model.Filters.eq;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

import model.Book;
import model.Student;
import model.User;

public class LibraryManagementSystem {
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> books;
    private final MongoCollection<Document> transactions;

    public LibraryManagementSystem(MongoDatabase db) {
        this.users = db.getCollection("users");
        this.books = db.getCollection("books");
        this.transactions = db.getCollection("transactions");
    }

    // =========== User operations ===========

    public void addUser(User user) {
        if (user == null) throw new IllegalArgumentException("user is null");

        // ensure username is unique
        Document existing = users.find(eq("username", user.getUsername())).first();
        if (existing != null) {
            throw new IllegalArgumentException("username already exists: " + user.getUsername());
        }

        // validate Student-specific fields
        if (user instanceof Student) {
            Student s = (Student) user;
            String sn = s.getStudentNumber();
            if (sn == null || sn.trim().isEmpty()) {
                throw new IllegalArgumentException("studentNumber is required for Student users");
            }
        }

        users.insertOne(user.toDocument());
    }

    // Convenience method to create and persist a Student with validation
    public Student addStudent(String id, String username, String password, String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("studentNumber is required");
        }

        String finalId = (id == null || id.trim().isEmpty()) ? ("u-" + UUID.randomUUID().toString()) : id;

        Student s = new Student(finalId, username, password, studentNumber);
        addUser(s);
        return s;
    }

    public boolean removeUserByUsername(String username) {
        DeleteResult res = users.deleteOne(eq("username", username));
        return res.getDeletedCount() > 0;
    }

    public User findUserByUsername(String username) {
        Document doc = users.find(eq("username", username)).first();
        return User.fromDocument(doc);
    }

    public boolean authenticate(String username, String password) {
        Document doc = users.find(eq("username", username)).first();
        if (doc == null) return false;
        String stored = doc.getString("password");
        return stored != null && stored.equals(password);
    }

    public List<User> listAllUsers() {
        List<User> out = new ArrayList<>();
        for (Document d : users.find()) {
            User u = User.fromDocument(d);
            if (u != null) out.add(u);
        }
        return out;
    }

    // =========== Book operations ===========

    public void addBook(Book b) {
        books.insertOne(b.toDocument());
    }

    public boolean removeBookByIsbn(String isbn) {
        DeleteResult res = books.deleteOne(eq("isbn", isbn));
        return res.getDeletedCount() > 0;
    }

    public Book findBookByIsbn(String isbn) {
        Document doc = books.find(eq("isbn", isbn)).first();
        return Book.fromDocument(doc);
    }

    public List<Book> listAllBooks() {
        List<Book> out = new ArrayList<>();
        for (Document d : books.find()) {
            Book b = Book.fromDocument(d);
            if (b != null) out.add(b);
        }
        return out;
    }

    // Borrow for printed books (reduce copiesAvailable and log transaction)
    public boolean borrowPrintedBook(String isbn, String username, int daysDue) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String type = doc.getString("type");
        if (!"printed".equalsIgnoreCase(type)) return false;

        Integer copies = doc.getInteger("copiesAvailable", 0);
        if (copies == null || copies <= 0) return false;

        // decrement copies and update isAvailable
        books.updateOne(eq("isbn", isbn),
                Updates.combine(
                        Updates.set("copiesAvailable", copies - 1),
                        Updates.set("isAvailable", (copies - 1) > 0)
                ));

        Document tx = new Document("action", "borrow")
                .append("isbn", isbn)
                .append("username", username)
                .append("timestamp", Instant.now().toString())
                .append("dueDate", Instant.now().plusSeconds(daysDue * 86400L).toString());
        transactions.insertOne(tx);
        return true;
    }

    // Return printed book (increment copies and log)
    public boolean returnPrintedBook(String isbn, String username) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String type = doc.getString("type");
        if (!"printed".equalsIgnoreCase(type)) return false;

        Integer copies = doc.getInteger("copiesAvailable", 0);

        books.updateOne(eq("isbn", isbn),
                Updates.combine(
                        Updates.set("copiesAvailable", copies + 1),
                        Updates.set("isAvailable", (copies + 1) > 0)
                ));

        Document tx = new Document("action", "return")
                .append("isbn", isbn)
                .append("username", username)
                .append("timestamp", Instant.now().toString());
        transactions.insertOne(tx);
        return true;
    }

    // Log access to an ebook (no change in availability)
    public boolean accessEBook(String isbn, String username) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String type = doc.getString("type");
        if (!"ebook".equalsIgnoreCase(type)) return false;

        Document tx = new Document("action", "access-ebook")
                .append("isbn", isbn)
                .append("username", username)
                .append("timestamp", Instant.now().toString());
        transactions.insertOne(tx);
        return true;
    }

    public List<Document> listTransactions() {
        List<Document> out = new ArrayList<>();
        for (Document d : transactions.find()) {
            out.add(d);
        }
        return out;
    }

    // Convenience wrappers used by the CLI Main
    public User login(String username, String password) {
        return authenticate(username, password) ? findUserByUsername(username) : null;
    }

    public boolean removeUser(String username) {
        return removeUserByUsername(username);
    }

    public void listBooks() {
        List<Book> bs = listAllBooks();
        for (Book b : bs) {
            System.out.println(b);
        }
    }

    public boolean borrowBook(String isbn, String username) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String type = doc.getString("type");
        if ("printed".equalsIgnoreCase(type)) {
            return borrowPrintedBook(isbn, username, 14); // default 14 days
        } else if ("ebook".equalsIgnoreCase(type)) {
            return accessEBook(isbn, username);
        }
        return false;
    }

    public boolean returnBook(String isbn, String username) {
        Document doc = books.find(eq("isbn", isbn)).first();
        if (doc == null) return false;
        String type = doc.getString("type");
        if ("printed".equalsIgnoreCase(type)) {
            return returnPrintedBook(isbn, username);
        }
        // nothing to do for ebooks
        return false;
    }
}