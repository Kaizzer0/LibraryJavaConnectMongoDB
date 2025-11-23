# Product Management System (Java + MongoDB)

Simple console application to manage products stored in MongoDB. Uses Java 17, Maven and the synchronous MongoDB driver.

## Quick links
- Project descriptor: [pom.xml](pom.xml)  
- Main CLI: [`com.example.demo.Main`](src/main/java/com/example/demo/Main.java)  
- Product model: [`com.example.demo.Product`](src/main/java/com/example/demo/Product.java)  
- App constants: [`com.example.demo.MyConstants`](src/main/java/com/example/demo/MyConstants.java)  
- Mongo connection (app): [`com.example.demo.MongoDBConnection`](src/main/java/com/example/demo/MongoDBConnection.java)  
- Mongo connection (service package): [`service.MongoDBConnection`](src/main/java/service/MongoDBConnection.java)  
- Library services (additional features): [`service.LibraryManagementSystem`](src/main/java/service/LibraryManagementSystem.java)

## Requirements
- Java 17 JDK
- Maven
- MongoDB server running (default: localhost:27017)

## Build & run
1. Start MongoDB (mongod).
2. Build:
   mvn -DskipTests compile
3. Run the CLI:
   mvn -DskipTests compile exec:java

Or run the main class directly:
   mvn exec:java -Dexec.mainClass=com.example.demo.Main

(See [pom.xml](pom.xml) for dependencies and exec plugin config.)

## Features
- Insert a product (unique `code`, `name`, `price`)
- List all products
- Query product by `code`
- Update product price by `code`
- Delete product by `code`
- Graceful shutdown closes MongoDB connection and Scanner

Key implementation points:
- MongoDB settings live in [`com.example.demo.MyConstants`](src/main/java/com/example/demo/MyConstants.java)
- Mongo client helper: [`com.example.demo.MongoDBConnection`](src/main/java/com/example/demo/MongoDBConnection.java)
- Product mapping: [`com.example.demo.Product`](src/main/java/com/example/demo/Product.java)
- CLI logic: [`com.example.demo.Main`](src/main/java/com/example/demo/Main.java)

## Example session
1. Start app
2. Choose `1` → Insert Product
   - code: `P001`
   - name: `Widget`
   - price: `9.99`
   - Output: `Inserted: Product{code='P001', name='Widget', price=9.99}`
3. Choose `2` → List All Products
   - Shows inserted products
4. Choose `3` → Query by Code `P001`
5. Choose `4` → Update Price for `P001` to `8.50`
6. Choose `5` → Delete product `P001`
7. Choose `6` → Exit

## Helpful commands (mongosh)
```
use ProductDB
db.products.find().pretty()
db.products.createIndex({ code: 1 }, { unique: true })
```

## Tips
- Ensure files are saved as UTF-8 (no BOM) to avoid compile errors.
- Create a unique index on `code` in MongoDB to enforce uniqueness at DB level.
- If using the library service, ensure the `LibraryDB` is available (`service.MongoDBConnection`).

## Troubleshooting
- Compilation errors: verify JDK 17 is used (`java -version`, `mvn -v`).
- Connection errors: ensure mongod is running and reachable at the URI in [`com.example.demo.MyConstants`](src/main/java/com/example/demo/MyConstants.java).
- If you get encoding errors for source files, reopen and save as UTF-8 without BOM in your editor.

