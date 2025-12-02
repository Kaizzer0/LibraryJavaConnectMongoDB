package com.example.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private MongoClient client;
    private MongoDatabase database;

    // Constructor mặc định (cho code cũ nếu cần)
    public MongoDBConnection() {
        this("mongodb://localhost:27017", "ProductDB");
    }

    // Constructor mới (Cho giao diện JavaFX)
    public MongoDBConnection(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(dbName);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        if (client != null) client.close();
    }
}