package com.example.demo;

public final class MyConstants {
    public static final String HOST = "localhost";
    public static final int PORT = 27017;
    public static final String DB_NAME = "ProductDB";
    public static final String URI = String.format("mongodb://%s:%d", HOST, PORT);

    private MyConstants() {}
}