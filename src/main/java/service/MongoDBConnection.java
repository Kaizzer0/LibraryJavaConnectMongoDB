package service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoDBConnection() {
        // connect to localhost:27017 and use database "LibraryDB"
        this.client = MongoClients.create("mongodb://localhost:27017");
        this.database = client.getDatabase("LibraryDB");
    }

    // convenience ctor that still allows overriding if needed
    public MongoDBConnection(String connectionString, String dbName) {
        String conn = (connectionString == null || connectionString.isEmpty()) ? "mongodb://localhost:27017" : connectionString;
        String db = (dbName == null || dbName.isEmpty()) ? "LibraryDB" : dbName;
        this.client = MongoClients.create(conn);
        this.database = client.getDatabase(db);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        client.close();
    }
}