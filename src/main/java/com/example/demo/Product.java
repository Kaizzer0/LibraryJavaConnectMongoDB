package com.example.demo;

import org.bson.Document;

public class Product {
    private String code;
    private String name;
    private double price;

    public Product() {}

    public Product(String code, String name, double price) {
        this.code = code;
        this.name = name;
        this.price = price;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Document toDocument() {
        Document d = new Document();
        if (code != null) d.append("code", code);
        d.append("name", name)
         .append("price", price);
        return d;
    }

    public static Product fromDocument(Document d) {
        if (d == null) return null;
        Product p = new Product();
        p.setCode(d.getString("code"));
        p.setName(d.getString("name"));
        Number n = d.get("price", Number.class);
        p.setPrice(n == null ? 0.0 : n.doubleValue());
        return p;
    }

    @Override
    public String toString() {
        return String.format("Product{code='%s', name='%s', price=%.2f}", code, name, price);
    }
}