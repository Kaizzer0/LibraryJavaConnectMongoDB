package com.example.demo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import org.bson.Document;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        MongoDBConnection conn = new MongoDBConnection();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Shutting down: closing MongoDB connection and scanner...");
            try { conn.close(); } catch (Exception ignored) {}
            try { scanner.close(); } catch (Exception ignored) {}
        }));

        MongoDatabase db = conn.getDatabase();
        MongoCollection<Document> products = db.getCollection("products");

        System.out.println("=== Product Management System ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": insertProduct(products); break;
                    case "2": listProducts(products); break;
                    case "3": queryByCode(products); break;
                    case "4": updatePrice(products); break;
                    case "5": deleteProduct(products); break;
                    case "6": running = false; break;
                    default: System.out.println("Invalid selection."); break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Exiting.");
    }

    private static void printMenu() {
        System.out.println("\n1) Insert Product");
        System.out.println("2) List All Products");
        System.out.println("3) Query by Code");
        System.out.println("4) Update Price");
        System.out.println("5) Delete Product");
        System.out.println("6) Exit");
        System.out.print("Choice: ");
    }

    private static void insertProduct(MongoCollection<Document> col) {
        System.out.print("Enter code: ");
        String code = scanner.nextLine().trim();
        if (code.isEmpty()) {
            System.out.println("Code cannot be empty.");
            return;
        }
        Document existing = col.find(eq("code", code)).first();
        if (existing != null) {
            System.out.println("A product with that code already exists.");
            return;
        }

        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();
        double price;
        Double p = readDouble("Enter price: ");
        if (p == null) return;
        price = p;

        Product product = new Product(code, name, price);
        col.insertOne(product.toDocument());
        System.out.println("Inserted: " + product);
    }

    private static void listProducts(MongoCollection<Document> col) {
        System.out.println("\n-- Products --");
        for (Document d : col.find()) {
            Product p = Product.fromDocument(d);
            System.out.println(p);
        }
    }

    private static void queryByCode(MongoCollection<Document> col) {
        System.out.print("Enter code: ");
        String code = scanner.nextLine().trim();
        Document d = col.find(eq("code", code)).first();
        Product p = Product.fromDocument(d);
        if (p == null) System.out.println("Not found.");
        else System.out.println("Found: " + p);
    }

    private static void updatePrice(MongoCollection<Document> col) {
        System.out.print("Enter code to update: ");
        String code = scanner.nextLine().trim();
        Document d = col.find(eq("code", code)).first();
        if (d == null) {
            System.out.println("Product not found.");
            return;
        }
        Double newPrice = readDouble("Enter new price: ");
        if (newPrice == null) return;
        UpdateResult res = col.updateOne(eq("code", code), set("price", newPrice));
        System.out.println("Matched: " + res.getMatchedCount() + ", Modified: " + res.getModifiedCount());
    }

    private static void deleteProduct(MongoCollection<Document> col) {
        System.out.print("Enter code to delete: ");
        String code = scanner.nextLine().trim();
        DeleteResult res = col.deleteOne(eq("code", code));
        System.out.println(res.getDeletedCount() > 0 ? "Deleted." : "No product deleted.");
    }

    private static Double readDouble(String prompt) {
        System.out.print(prompt);
        String raw = scanner.nextLine().trim();
        try {
            double v = Double.parseDouble(raw);
            if (v < 0) {
                System.out.println("Value cannot be negative.");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return null;
        }
    }
}