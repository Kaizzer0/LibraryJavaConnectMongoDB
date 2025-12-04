package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import service.LibraryManagementSystem;
import service.MongoDBConnection;
import model.User;
import model.Librarian;
import model.Reader;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {
    
    @FXML private TextField uriField, dbField, collectionField, searchField;
    @FXML private TableView<Document> table;
    @FXML private TableColumn<Document, String> colCode, colName, colCategory, colDateAdded, colStatus;

    @FXML private Button addButton, updateButton, deleteButton, userMgmtButton, loginButton;
    @FXML private Button btnBorrow, btnReturn;
    @FXML private Label userLabel;

    private MongoDBConnection conn;
    private MongoCollection<Document> collection;
    private ObservableList<Document> data = FXCollections.observableArrayList();

    private User currentUser;

    @FXML
    public void initialize() {
        uriField.setText("mongodb://localhost:27017");
        dbField.setText("ProductDB");
        collectionField.setText("products");
   

        colCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "code")));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "name")));
        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "category")));
        colDateAdded.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "createdDate", "lastUpdated")));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "status")));

        table.setItems(data);
        applyRoleVisibility("anonymous");
    }

    private String stringOf(Document d, String key) {
        if (d == null) return "";
        Object v = d.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private String stringOf(Document d, String key, String fallback) {
        if (d == null) return "";
        Object v = d.get(key);
        if (v != null) return String.valueOf(v);
        Object f = d.get(fallback);
        return f == null ? "" : String.valueOf(f);
    }

    @FXML
    private void onConnect() {
        try {
            close();
            conn = new MongoDBConnection(uriField.getText(), dbField.getText());
            collection = conn.getDatabase().getCollection(collectionField.getText());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Connected", "Connection successful");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Connection failed", e.getMessage());
        }
    }

    public void loadData() {
        if (collection == null) return;
        data.clear();
        for (Document d : collection.find()) data.add(d);
    }

    @FXML private void onRefresh() { loadData(); }

    @FXML private void onSearch() {
        if (collection == null) return;
        String q = searchField.getText().trim();
        if (q.isEmpty()) { loadData(); return; }
        data.clear();
        for (Document d : collection.find(Filters.or(Filters.regex("code", q, "i"), Filters.regex("name", q, "i"))))
            data.add(d);
    }

    @FXML
    private void onLogin() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField username = new TextField(); PasswordField password = new PasswordField();
        grid.addRow(0, new Label("Username:"), username);
        grid.addRow(1, new Label("Password:"), password);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> (bt == ButtonType.OK) ? new Pair<>(username.getText(), password.getText()) : null);

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(creds -> {
            MongoDBConnection svcConn = null;
            try {
                svcConn = new MongoDBConnection();
                LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                User user = lms.login(creds.getKey(), creds.getValue());
                if (user != null) {
                    initSession(user);
                    showAlert(Alert.AlertType.INFORMATION, "Welcome", "Hello " + user.getUsername() + " (" + user.getRole() + ")");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login failed", "Invalid credentials");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            } finally {
                try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {}
            }
        });
    }

    public void initSession(User user) {
        this.currentUser = user;
        String role = (user == null) ? "anonymous" : user.getRole();
        userLabel.setText(user == null ? "Not logged in" : "User: " + user.getUsername() + " (" + role + ")");
        applyRoleVisibility(role);
    }

    private void applyRoleVisibility(String roleIn) {
        String role = (roleIn == null) ? "anonymous" : roleIn.toLowerCase();

        setNodeVisible(addButton, false);
        setNodeVisible(updateButton, false);
        setNodeVisible(deleteButton, false);
        setNodeVisible(userMgmtButton, false);
        setNodeVisible(btnBorrow, false);
        setNodeVisible(btnReturn, false);

        switch (role) {
            case "admin":
                setNodeVisible(userMgmtButton, true);
                break;
            case "librarian":
                setNodeVisible(addButton, true);
                setNodeVisible(updateButton, true);
                setNodeVisible(deleteButton, true);
                break;
            case "reader":
            case "student":
                setNodeVisible(btnBorrow, true);
                setNodeVisible(btnReturn, true);
                break;
            default:
                break;
        }
    }

    private void setNodeVisible(Control c, boolean visible) { if (c != null) { c.setVisible(visible); c.setManaged(visible); } }

    private boolean hasRole(String... allowed) {
        if (currentUser == null) return false;
        String r = currentUser.getRole();
        if (r == null) return false;
        for (String a : allowed) if (a.equalsIgnoreCase(r)) return true;
        return false;
    }

    @FXML private void onAdd() {
        if (!hasRole("librarian")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only librarians can add books."); return; }
        if (collection == null) { showAlert(Alert.AlertType.WARNING, "Not connected", "Connect first"); return; }

        showEditDialog(null).ifPresent(doc -> {
            String isbn = doc.getString("isbn");
            String status = doc.getString("status");
            if (isbn == null || isbn.trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Invalid", "ISBN is required"); return; }
            if (status == null || status.trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Invalid", "Status is required"); return; }

            String now = Instant.now().toString();
            doc.append("createdDate", now);
            doc.append("lastUpdated", now);
            if (!doc.containsKey("status")) doc.append("status", "Available");
            doc.remove("price");
            collection.insertOne(doc);
            loadData();
        });
    }

    @FXML private void onUpdate() {
        if (!hasRole("librarian")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only librarians can update books."); return; }
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "No selection", "Select a row first"); return; }

        showEditDialog(sel).ifPresent(doc -> {
            String isbn = doc.getString("isbn");
            if (isbn == null || isbn.trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Invalid", "ISBN is required"); return; }
            String now = Instant.now().toString();
            doc.append("lastUpdated", now);
            doc.remove("price");
            collection.updateOne(Filters.eq("_id", sel.get("_id")),
                    Updates.combine(
                            Updates.set("name", doc.getString("name")),
                            Updates.set("category", doc.getString("category")),
                            Updates.set("isbn", doc.getString("isbn")),
                            Updates.set("status", doc.getString("status")),
                            Updates.set("lastUpdated", doc.getString("lastUpdated")),
                            Updates.set("borrowedBy", doc.get("borrowedBy"))
                    ));
            loadData();
        });
    }

    @FXML private void onDelete() {
        if (!hasRole("librarian")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only librarians can delete books."); return; }
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "No selection", "Select a row first"); return; }
        Alert cf = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + sel.getString("name") + "?", ButtonType.YES, ButtonType.NO);
        if (cf.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            collection.deleteOne(Filters.eq("_id", sel.get("_id")));
            loadData();
        }
    }

    @FXML private void onBorrow() {
        if (!hasRole("reader", "student")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only readers can borrow books."); return; }
        if (currentUser == null) { showAlert(Alert.AlertType.WARNING, "Not logged in", "Please login first."); return; }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Borrow Book");
        dlg.setHeaderText("Enter Book Title to borrow");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(title -> {
            MongoDBConnection svcConn = null;
            try {
                svcConn = new MongoDBConnection();
                LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                boolean ok = lms.borrowBook(title.trim(), currentUser.getUsername());
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, ok ? "Borrowed" : "Failed",
                        ok ? "Book borrowed successfully." : "No available book with that title.");
                if (ok) loadData();
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            finally { try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {} }
        });
    }

    @FXML private void onReturn() {
        if (!hasRole("reader", "student")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only readers can return books."); return; }
        if (currentUser == null) { showAlert(Alert.AlertType.WARNING, "Not logged in", "Please login first."); return; }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Return Book");
        dlg.setHeaderText("Enter Book Title to return");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(title -> {
            MongoDBConnection svcConn = null;
            try {
                svcConn = new MongoDBConnection();
                LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                boolean ok = lms.returnBook(title.trim(), currentUser.getUsername());
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, ok ? "Returned" : "Failed",
                        ok ? "Book returned successfully." : "Return failed (no matching borrowed book found for you).");
                if (ok) loadData();
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            finally { try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {} }
        });
    }

    @FXML private void onUserManagement() {
        if (!hasRole("admin")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only admin can manage users."); return; }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("User Management");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);

        ListView<String> usersList = new ListView<>();
        Button btnRefresh = new Button("Refresh");
        Button btnDelete = new Button("Delete Selected");
        Button btnCreate = new Button("Create User");

        g.addRow(0, new Label("Users:"), usersList);
        g.addRow(1, btnRefresh, btnDelete, btnCreate);

        dlg.getDialogPane().setContent(g);

        Runnable loadUsers = () -> {
            MongoDBConnection svcConn = null;
            try {
                svcConn = new MongoDBConnection();
                LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                List<String> all = lms.listAllUsers().stream().map(User::getUsername).collect(Collectors.toList());
                usersList.getItems().setAll(all);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            } finally { try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {} }
        };

        btnRefresh.setOnAction(ev -> loadUsers.run());

        btnDelete.setOnAction(ev -> {
            String sel = usersList.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert(Alert.AlertType.WARNING, "No selection", "Select a user to delete"); return; }
            if (currentUser != null && sel.equals(currentUser.getUsername())) { showAlert(Alert.AlertType.WARNING, "Invalid", "You cannot delete yourself"); return; }
            Alert cf = new Alert(Alert.AlertType.CONFIRMATION, "Delete user " + sel + "?", ButtonType.YES, ButtonType.NO);
            if (cf.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                MongoDBConnection svcConn = null;
                try {
                    svcConn = new MongoDBConnection();
                    LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                    boolean ok = lms.deleteUserByUsername(sel);
                    showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, ok ? "Deleted" : "Failed", ok ? "User deleted" : "Delete failed");
                    loadUsers.run();
                } catch (Exception ex) { showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
                finally { try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {} }
            }
        });

        btnCreate.setOnAction(ev -> {
            Dialog<ButtonType> cd = new Dialog<>();
            cd.setTitle("Create User");
            cd.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            GridPane cg = new GridPane(); cg.setHgap(10); cg.setVgap(10);
            TextField username = new TextField(); PasswordField password = new PasswordField();
            ChoiceBox<String> roleBox = new ChoiceBox<>(FXCollections.observableArrayList("librarian", "reader", "student")); // admin removed
            TextField studentNumber = new TextField();
            studentNumber.setPromptText("studentNumber (if student)");

            cg.addRow(0, new Label("Username:"), username);
            cg.addRow(1, new Label("Password:"), password);
            cg.addRow(2, new Label("Role:"), roleBox);
            cg.addRow(3, new Label("Student #:"), studentNumber);

            cd.getDialogPane().setContent(cg);

            Optional<ButtonType> cre = cd.showAndWait();
            if (cre.isPresent() && cre.get() == ButtonType.OK) {
                String u = username.getText().trim();
                String p = password.getText().trim();
                String role = roleBox.getValue();
                String sn = studentNumber.getText().trim();
                if (u.isEmpty() || p.isEmpty() || role == null) { showAlert(Alert.AlertType.WARNING, "Invalid", "Provide username, password, role"); return; }

                MongoDBConnection svcConn = null;
                try {
                    svcConn = new MongoDBConnection();
                    LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                    switch (role.toLowerCase()) {
                        case "librarian": lms.addUser(new Librarian("u-"+u, u, p)); break;
                        case "student":
                            if (sn.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Invalid", "studentNumber required for student"); return; }
                            lms.addStudent("u-"+u, u, p, sn);
                            break;
                        default: lms.addUser(new Reader("u-"+u, u, p)); break;
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Created", "User created: " + u + " (" + role + ")");
                    loadUsers.run();
                } catch (Exception ex) { showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
                finally { try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {} }
            }
        });

        loadUsers.run();
        dlg.showAndWait();
    }

    private Optional<Document> showEditDialog(Document template) {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle(template == null ? "Add Book" : "Edit Book");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
        TextField code = new TextField(), name = new TextField(), cat = new TextField(), isbn = new TextField();
        ChoiceBox<String> statusBox = new ChoiceBox<>(FXCollections.observableArrayList("Available", "Borrowed"));
        TextField borrowedBy = new TextField(); borrowedBy.setPromptText("borrowedBy (optional)");

        code.setPromptText("Code"); name.setPromptText("Name"); cat.setPromptText("Category"); isbn.setPromptText("ISBN");
        statusBox.getSelectionModel().select("Available");

        if (template != null) {
            code.setText(stringOf(template, "code")); code.setDisable(true);
            name.setText(stringOf(template, "name"));
            cat.setText(stringOf(template, "category"));
            isbn.setText(stringOf(template, "isbn"));
            String st = stringOf(template, "status");
            if (!st.isEmpty()) statusBox.getSelectionModel().select(st);
            borrowedBy.setText(stringOf(template, "borrowedBy"));
        }

        g.addRow(0, new Label("Code:"), code);
        g.addRow(1, new Label("Name:"), name);
        g.addRow(2, new Label("Category:"), cat);
        g.addRow(3, new Label("ISBN:"), isbn);
        g.addRow(4, new Label("Status:"), statusBox);
        g.addRow(5, new Label("Borrowed By:"), borrowedBy);

        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Document d = new Document();
                d.append("code", code.getText())
                 .append("name", name.getText())
                 .append("category", cat.getText())
                 .append("isbn", isbn.getText())
                 .append("status", statusBox.getValue())
                 .append("borrowedBy", borrowedBy.getText().isEmpty() ? null : borrowedBy.getText());
                return d;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public void close() { if (conn != null) conn.close(); }
}