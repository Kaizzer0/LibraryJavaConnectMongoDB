package com.example.demo;

import service.MongoDBConnection;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class AppLauncher {
    public static void main(String[] args) {
        // --- RUN THIS ONCE TO SEED USERS ---
        MongoDBConnection c = null;
        try {
            // Initialize connection (assuming no-arg constructor or appropriate constructor exists)
            c = new MongoDBConnection(); 
            
            // Access the database and collection
            MongoDatabase db = c.getDatabase();
            MongoCollection<Document> usersCol = db.getCollection("users");

            // Check if users exist, if not, seed them
            if (usersCol.countDocuments() < 3) {
                // Seed Admin
                if (usersCol.find(new Document("username", "admin")).first() == null) {
                    usersCol.insertOne(new Document("username", "admin")
                            .append("password", "123")
                            .append("role", "admin"));
                }
                
                // Seed Librarian
                if (usersCol.find(new Document("username", "lib")).first() == null) {
                    usersCol.insertOne(new Document("username", "lib")
                            .append("password", "123")
                            .append("role", "librarian"));
                }
                
                // Seed Reader
                if (usersCol.find(new Document("username", "read")).first() == null) {
                    usersCol.insertOne(new Document("username", "read")
                            .append("password", "123")
                            .append("role", "reader"));
                }
                
                System.out.println(">>> Users seeded: admin, lib, read");
            }
            
        } catch (Exception e) {
            System.err.println("Error seeding users: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close connection manually in finally block
            if (c != null) {
                c.close();
            }
        }
        // ----------------------------------------

        // Launch the JavaFX application
        App.main(args);
    }
}