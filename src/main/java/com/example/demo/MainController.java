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
import service.MongoDBConnection; // ensure correct import
import model.User;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class MainController {

    // UI Elements from FXML
    @FXML private TextField uriField, dbField, collectionField, searchField;
    @FXML private TableView<Document> table;
    @FXML private TableColumn<Document, String> colCode, colName, colCategory, colPrice, colInStock;
    
    // RBAC Elements (buttons)
    @FXML private Button addButton, updateButton, deleteButton, userMgmtButton, loginButton;
    @FXML private Label userLabel;

    // Database Variables
    private MongoDBConnection conn;
    private MongoCollection<Document> collection;
    private ObservableList<Document> data = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    
    // Session Variable - used by handlers and initSession
    private User currentUser;

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "code")));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "name")));
        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "category")));
        colPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formatPrice(c.getValue())));
        colInStock.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(booleanOf(c.getValue(), "inStock"))));
        
        table.setItems(data);

        // Default: not logged in -> hide management controls (reader mode)
        applyRoleVisibility("anonymous");
    }

    private String stringOf(Document d, String k) { Object v = d==null?null:d.get(k); return v==null?"":String.valueOf(v); }
    private boolean booleanOf(Document d, String k) { if(d==null) return false; Object v=d.get(k); if (v instanceof Boolean) return (Boolean)v; if (v instanceof Number) return ((Number)v).intValue() != 0; return false; }
    private String formatPrice(Document d) { 
        if(d==null) return ""; 
        try { Object p = d.get("price"); if (p instanceof Number) return currencyFormat.format(((Number)p).doubleValue()); return currencyFormat.format(Double.parseDouble(String.valueOf(p))); } catch(Exception e) { return ""; }
    }

    @FXML
    private void onConnect() {
        try {
            close();
            conn = new MongoDBConnection(uriField.getText(), dbField.getText());
            collection = conn.getDatabase().getCollection(collectionField.getText());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Connected!");
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Connection Failed", e.getMessage()); }
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

    // Login dialog - accepts any valid user role returned by the DB
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
            service.MongoDBConnection svcConn = null;
            try {
                svcConn = new service.MongoDBConnection(); // LibraryDB (for users)
                LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());
                User user = lms.login(creds.getKey(), creds.getValue());
                if (user != null) {
                    initSession(user); // accept any role returned
                    showAlert(Alert.AlertType.INFORMATION, "Welcome", "Hello " + user.getUsername() + " (" + user.getRole() + ")");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Wrong username or password");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            } finally {
                try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {}
            }
        });
    }

    // Initialize session + apply RBAC
    public void initSession(User user) {
        this.currentUser = user;
        String role = (user == null) ? "anonymous" : user.getRole();
        userLabel.setText(user == null ? "Not logged in" : "User: " + user.getUsername() + " (" + role.toLowerCase() + ")");
        applyRoleVisibility(role);
    }

    // RBAC: admin -> all; librarian -> add+update; reader -> none (read-only)
    private void applyRoleVisibility(String roleIn) {
        String role = (roleIn == null) ? "anonymous" : roleIn.toLowerCase();

        // hide everything by default
        setNodeVisible(addButton, false);
        setNodeVisible(updateButton, false);
        setNodeVisible(deleteButton, false);
        setNodeVisible(userMgmtButton, false);

        switch (role) {
            case "admin":
                setNodeVisible(addButton, true);
                setNodeVisible(updateButton, true);
                setNodeVisible(deleteButton, true);
                setNodeVisible(userMgmtButton, true);
                break;
            case "librarian":
                setNodeVisible(addButton, true);
                setNodeVisible(updateButton, true);
                // delete and userMgmt remain hidden
                break;
            case "reader":
            case "student":
            case "anonymous":
            default:
                // read-only: all management buttons hidden
                break;
        }
    }

    private void setNodeVisible(Control c, boolean visible) {
        if (c != null) { c.setVisible(visible); c.setManaged(visible); }
    }

    // Helper for runtime checks (ensures currentUser is used)
    private boolean hasRole(String... allowed) {
        if (currentUser == null) return false;
        String r = currentUser.getRole();
        if (r == null) return false;
        for (String a : allowed) if (a.equalsIgnoreCase(r)) return true;
        return false;
    }

    // CRUD handlers with runtime RBAC enforcement (uses currentUser)
    @FXML private void onAdd() {
        if (collection == null) { showAlert(Alert.AlertType.WARNING, "Not connected", "Connect first"); return; }
        if (!hasRole("admin", "librarian")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "You are not allowed to add items."); return; }
        showEditDialog(null).ifPresent(doc -> {
            if (collection.find(Filters.eq("code", doc.getString("code"))).first() != null) {
                showAlert(Alert.AlertType.WARNING, "Error", "Code exists!"); return;
            }
            collection.insertOne(doc); loadData();
        });
    }

    @FXML private void onUpdate() {
        if (!hasRole("admin", "librarian")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "You are not allowed to update items."); return; }
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "No selection", "Select a row first"); return; }
        showEditDialog(sel).ifPresent(doc -> {
            collection.updateOne(Filters.eq("code", doc.getString("code")),
                Updates.combine(
                    Updates.set("name", doc.getString("name")),
                    Updates.set("price", doc.get("price")),
                    Updates.set("category", doc.getString("category")),
                    Updates.set("inStock", doc.getBoolean("inStock"))
                ));
            loadData();
        });
    }

    @FXML private void onDelete() {
        if (!hasRole("admin")) { showAlert(Alert.AlertType.WARNING, "Permission denied", "Only admin can delete items."); return; }
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "No selection", "Select a row first"); return; }
        Alert cf = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + sel.getString("code") + "?", ButtonType.YES, ButtonType.NO);
        if (cf.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            collection.deleteOne(Filters.eq("code", sel.getString("code")));
            loadData();
        }
    }

    // Edit dialog
    private Optional<Document> showEditDialog(Document template) {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle(template == null ? "Add Product" : "Edit Product");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
        TextField code = new TextField(), name = new TextField(), cat = new TextField(), price = new TextField();
        CheckBox inStock = new CheckBox();

        code.setPromptText("Code"); name.setPromptText("Name"); cat.setPromptText("Category"); price.setPromptText("Price");

        if (template != null) {
            code.setText(stringOf(template, "code")); code.setDisable(true);
            name.setText(stringOf(template, "name"));
            cat.setText(stringOf(template, "category"));
            price.setText(stringOf(template, "price"));
            inStock.setSelected(booleanOf(template, "inStock"));
        }

        g.addRow(0, new Label("Code:"), code); g.addRow(1, new Label("Name:"), name);
        g.addRow(2, new Label("Category:"), cat); g.addRow(3, new Label("Price:"), price);
        g.addRow(4, new Label("In Stock:"), inStock);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return new Document("code", code.getText()).append("name", name.getText())
                            .append("category", cat.getText()).append("price", Double.parseDouble(price.getText()))
                            .append("inStock", inStock.isSelected());
                } catch (Exception e) { return null; }
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