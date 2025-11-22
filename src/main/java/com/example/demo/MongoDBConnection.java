package com.example.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoDBConnection() {
        this(MyConstants.URI, MyConstants.DB_NAME);
    }

    public MongoDBConnection(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(dbName);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        try { client.close(); } catch (Exception ignored) {}
    }
}