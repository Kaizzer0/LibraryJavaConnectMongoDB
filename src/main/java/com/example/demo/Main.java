package com.example.demo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import java.util.Scanner;

import model.Admin;
import model.Librarian;
import model.Reader;
import service.LibraryManagementSystem;

public class Main {
    // keep scanner non-final so we can close it
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Ensure default users exist in LibraryDB (admin, lib, read)
        seedData();

        MongoDBConnection conn = new MongoDBConnection();
        MongoDatabase db = conn.getDatabase();
        MongoCollection<Document> products = db.getCollection("products");

        System.out.println("=== Product Management System ===");

        try {
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
        } finally {
            // Explicitly close resources while classes are available
            System.out.println("[System] Closing MongoDB connection and scanner...");
            try { conn.close(); } catch (Throwable t) { System.err.println("Close error: " + t.getMessage()); }
            try { if (scanner != null) scanner.close(); } catch (Throwable ignored) {}
        }

        System.out.println("Exiting.");
    }

    /**
     * Seed default users into LibraryDB if they do not already exist:
     *  - admin / 123 / admin
     *  - lib   / 123 / librarian
     *  - read  / 123 / reader
     *
     * Uses service.MongoDBConnection (LibraryDB) and LibraryManagementSystem to add users.
     */
    private static void seedData() {
        service.MongoDBConnection svcConn = null;
        try {
            svcConn = new service.MongoDBConnection(); // connects to LibraryDB by default
            LibraryManagementSystem lms = new LibraryManagementSystem(svcConn.getDatabase());

            // admin
            if (lms.findUserByUsername("admin") == null) {
                try {
                    lms.addUser(new Admin("u-admin", "admin", "123"));
                    System.out.println("[seed] created user: admin / role=admin");
                } catch (Exception ex) { System.out.println("[seed] admin create failed: " + ex.getMessage()); }
            }

            // librarian
            if (lms.findUserByUsername("lib") == null) {
                try {
                    lms.addUser(new Librarian("u-lib", "lib", "123"));
                    System.out.println("[seed] created user: lib / role=librarian");
                } catch (Exception ex) { System.out.println("[seed] lib create failed: " + ex.getMessage()); }
            }

            // reader
            if (lms.findUserByUsername("read") == null) {
                try {
                    lms.addUser(new Reader("u-read", "read", "123"));
                    System.out.println("[seed] created user: read / role=reader");
                } catch (Exception ex) { System.out.println("[seed] read create failed: " + ex.getMessage()); }
            }
        } catch (Exception e) {
            System.err.println("[seed] error: " + e.getMessage());
        } finally {
            try { if (svcConn != null) svcConn.close(); } catch (Exception ignored) {}
        }
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
        Double p = readDouble("Enter price: ");
        if (p == null) return;
        Product product = new Product(code, name, p);
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