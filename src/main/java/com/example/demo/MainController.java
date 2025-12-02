package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import service.MongoDBConnection;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class MainController {
    @FXML private TextField uriField;
    @FXML private TextField dbField;
    @FXML private TextField collectionField;
    @FXML private TextField searchField;
    @FXML private TableView<Document> table;
    @FXML private TableColumn<Document, String> colCode;
    @FXML private TableColumn<Document, String> colName;
    @FXML private TableColumn<Document, String> colCategory;
    @FXML private TableColumn<Document, String> colPrice;
    @FXML private TableColumn<Document, String> colInStock;

    private MongoDBConnection conn;
    private MongoCollection<Document> collection;
    private ObservableList<Document> data = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    public void initialize() {
        uriField.setText("mongodb://localhost:27017");
        dbField.setText("ProductDB");
        collectionField.setText("products");

        colCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "code")));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "name")));
        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(stringOf(c.getValue(), "category")));
        colPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formatPrice(c.getValue())));
        colInStock.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(booleanOf(c.getValue(), "inStock"))));

        table.setItems(data);
    }

    private String stringOf(Document d, String k) { Object v = d==null?null:d.get(k); return v==null?"":String.valueOf(v); }
    private boolean booleanOf(Document d, String k) {
        if (d==null) return false; Object v = d.get(k); if (v instanceof Boolean) return (Boolean)v; if (v instanceof Number) return ((Number)v).intValue()!=0; return false;
    }
    private String formatPrice(Document d) {
        if (d==null) return "";
        Object p = d.get("price");
        if (p instanceof Number) return currencyFormat.format(((Number)p).doubleValue());
        try { return currencyFormat.format(Double.parseDouble(String.valueOf(p))); } catch (Exception e) { return ""; }
    }

    @FXML
    private void onConnect() {
        String uri = uriField.getText().trim();
        String dbn = dbField.getText().trim();
        String coll = collectionField.getText().trim();
        if (uri.isEmpty() || dbn.isEmpty() || coll.isEmpty()) { warn("Fill all fields"); return; }
        try {
            close();
            conn = new MongoDBConnection(uri, dbn);
            MongoDatabase db = conn.getDatabase();
            collection = db.getCollection(coll);
            loadData();
            info("Connected");
        } catch (Exception e) { error("Connection failed: " + e.getMessage()); }
    }

    public void loadData() {
        if (collection == null) return;
        data.clear();
        for (Document d : collection.find()) data.add(d);
    }

    @FXML
    private void onRefresh() { loadData(); }

    @FXML
    private void onSearch() {
        if (collection == null) { warn("Not connected"); return; }
        String q = searchField.getText().trim();
        if (q.isEmpty()) { loadData(); return; }
        data.clear();
        for (Document d : collection.find(
                Filters.or(Filters.regex("code", q, "i"), Filters.regex("name", q, "i"))
        )) data.add(d);
    }

    @FXML
    private void onAdd() {
        if (collection == null) { warn("Not connected"); return; }
        Optional<Document> res = showEditDialog(null);
        res.ifPresent(doc -> {
            if (collection.find(Filters.eq("code", doc.getString("code"))).first() != null) { warn("Code exists"); return; }
            collection.insertOne(doc); loadData();
        });
    }

    @FXML
    private void onUpdate() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select row"); return; }
        Optional<Document> res = showEditDialog(sel);
        res.ifPresent(doc -> {
            String code = doc.getString("code");
            collection.updateOne(Filters.eq("code", code),
                    Updates.combine(
                            Updates.set("name", doc.getString("name")),
                            Updates.set("price", doc.get("price")),
                            Updates.set("category", doc.getString("category")),
                            Updates.set("inStock", doc.getBoolean("inStock", false))
                    ));
            loadData();
        });
    }

    @FXML
    private void onDelete() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select row"); return; }
        String code = sel.getString("code");
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + code + "?", ButtonType.YES, ButtonType.NO);
        if (a.showAndWait().filter(bt -> bt == ButtonType.YES).isPresent()) {
            collection.deleteOne(Filters.eq("code", code)); loadData();
        }
    }

    private Optional<Document> showEditDialog(Document tmpl) {
        Dialog<Document> dialog = new Dialog<>(); dialog.setTitle(tmpl==null? "Add":"Edit");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
        TextField code = new TextField(); TextField name = new TextField(); TextField cat = new TextField(); TextField price = new TextField(); CheckBox inStock = new CheckBox();
        if (tmpl != null) { code.setText(stringOf(tmpl,"code")); name.setText(stringOf(tmpl,"name")); cat.setText(stringOf(tmpl,"category")); price.setText(stringOf(tmpl,"price")); inStock.setSelected(booleanOf(tmpl,"inStock")); code.setDisable(true); }
        g.addRow(0, new Label("Code:"), code); g.addRow(1, new Label("Name:"), name); g.addRow(2, new Label("Category:"), cat); g.addRow(3, new Label("Price:"), price); g.addRow(4, new Label("In Stock:"), inStock);
        dialog.getDialogPane().setContent(g);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (name.getText().trim().isEmpty()) { warn("Name required"); ev.consume(); return; }
            try { double v = Double.parseDouble(price.getText().trim()); if (v < 0) { warn("Price >= 0"); ev.consume(); } } catch (Exception ex) { warn("Invalid price"); ev.consume(); }
        });
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Document doc = new Document();
                doc.append("code", code.getText().trim());
                doc.append("name", name.getText().trim());
                doc.append("category", cat.getText().trim());
                try { doc.append("price", Double.parseDouble(price.getText().trim())); } catch (Exception e) { doc.append("price", 0); }
                doc.append("inStock", inStock.isSelected()); return doc;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void warn(String m) { show(Alert.AlertType.WARNING, "Warning", m); }
    private void error(String m) { show(Alert.AlertType.ERROR, "Error", m); }
    private void info(String m) { show(Alert.AlertType.INFORMATION, "Info", m); }
    private void show(Alert.AlertType t, String title, String m) { Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }

    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}