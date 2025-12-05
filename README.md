# Product & Library Management System (Java 21 + MongoDB + JavaFX)

A dual-mode application with CLI console and JavaFX GUI for managing products and library books using MongoDB.

## Features

### Console Mode (CLI)
- Insert, list, query, update, and delete products
- Unique product code validation
- Price validation (non-negative)
- Direct MongoDB interaction via [`com.example.demo.Main`](src/main/java/com/example/demo/Main.java)

### JavaFX GUI Mode
- **Product Management** (Librarian role):
  - Add books with code, name, category, ISBN, status
  - Update existing books
  - Delete books
  - View products in a searchable table

- **User Management** (Admin role):
  - Create users (librarian, reader, student)
  - Delete users (except self)
  - Assign student numbers

- **Borrow/Return System** (Reader/Student roles):
  - Borrow available books by title
  - Return borrowed books
  - Track borrowed-by status

- **Search & Filter**:
  - Real-time search by code or name
  - Connection to any MongoDB URI and database

## Quick Start

### Prerequisites
- Java 21 JDK installed
- Maven 3.6+
- MongoDB server running on `localhost:27017` (or specify custom URI)

### Build
```sh
mvn clean compile
```

### Run Console (CLI)
```sh
mvn exec:java -Dexec.mainClass=com.example.demo.Main
```

**What it does:**
- Seeds default users (admin, lib, read) into LibraryDB
- Provides menu-driven product CRUD operations
- Direct [`Product`](src/main/java/com/example/demo/Product.java) and MongoDB interaction

**Main file:** [`src/main/java/com/example/demo/Main.java`](src/main/java/com/example/demo/Main.java)

### Run JavaFX GUI
```sh
mvn javafx:run
```

Or alternatively:
```sh
mvn exec:java -Dexec.mainClass=com.example.demo.AppLauncher
```

**What it does:**
- Launches JavaFX UI with connection fields pre-filled
- Default: URI=`mongodb://localhost:27017`, DB=`ProductDB`, Collection=`products`
- Seeds default users into LibraryDB if not present
- Provides login and role-based UI (Admin, Librarian, Reader, Student)

**Main files:**
- Launcher: [`src/main/java/com/example/demo/AppLauncher.java`](src/main/java/com/example/demo/AppLauncher.java)
- Controller: [`src/main/java/com/example/demo/MainController.java`](src/main/java/com/example/demo/MainController.java)
- FXML UI: [`src/main/resources/MainView.fxml`](src/main/resources/MainView.fxml)

---

## Usage Guide

### Console Mode

1. **Start the app:**
   ```sh
   mvn exec:java -Dexec.mainClass=com.example.demo.Main
   ```

2. **Menu options:**
   ```
   1) Insert Product
   2) List All Products
   3) Query by Code
   4) Update Price
   5) Delete Product
   6) Exit
   ```

3. **Insert a product:**
   - Enter code (unique, e.g., "P001")
   - Enter name (e.g., "Book A")
   - Enter price (e.g., 19.99)

4. **Example flow:**
   ```
   Choice: 1
   Enter code: P001
   Enter name: Java Basics
   Enter price: 25.50
   Inserted: Product{code='P001', name='Java Basics', price=25.50}
   ```

### JavaFX GUI Mode

1. **Start the app:**
   ```sh
   mvn javafx:run
   ```

2. **Login (Optional - for role-based features):**
   - Click **Login**
   - Default credentials:
     - Username: `admin` / Password: `123` (Admin role)
     - Username: `lib` / Password: `123` (Librarian role)
     - Username: `read` / Password: `123` (Reader role)

3. **Connect to MongoDB:**
   - URI field is pre-filled: `mongodb://localhost:27017`
   - DB field is pre-filled: `ProductDB`
   - Collection field is pre-filled: `products`
   - Click **Connect** to load data

4. **Role-Based Features:**

   **Librarian (after login as "lib"):**
   - Click **Add** to insert new books
   - Fill in: Code, Name, Category, ISBN, Status
   - Click **Update** to modify selected book
   - Click **Delete** to remove book

   **Reader/Student (after login as "read"):**
   - Click **Borrow** → Enter book title → Confirm
   - Click **Return** → Enter book title → Confirm
   - Status changes from "Available" to "Borrowed" in table

   **Admin (after login as "admin"):**
   - Click **User Mgmt (Admin)**
   - **Create User**: Select role (librarian/reader/student), set password
   - **Delete User**: Select and confirm deletion (cannot delete self)
   - **Refresh**: Reload user list

5. **Search:**
   - Type in search field by code or name
   - Click **Refresh** to reset filter

---

## Architecture

### Directory Structure
```
src/
├── main/
│   ├── java/
│   │   ├── com/example/demo/
│   │   │   ├── Main.java                    (CLI entry point)
│   │   │   ├── App.java                     (JavaFX Application class)
│   │   │   ├── AppLauncher.java            (Launcher with seeding)
│   │   │   ├── MainController.java         (UI controller)
│   │   │   ├── Product.java                (Product model)
│   │   │   ├── MongoDBConnection.java      (App DB connector for ProductDB)
│   │   │   ├── MyConstants.java            (App constants)
│   │   │   └── ... (other models)
│   │   ├── service/
│   │   │   ├── MongoDBConnection.java      (Service DB connector for LibraryDB)
│   │   │   └── LibraryManagementSystem.java (Business logic)
│   │   └── model/
│   │       ├── User.java
│   │       ├── Admin.java
│   │       ├── Librarian.java
│   │       ├── Reader.java
│   │       ├── Student.java
│   │       └── Book.java
│   └── resources/
│       └── MainView.fxml                   (JavaFX UI layout)
└── test/
    └── ... (tests)
```

### Database Schema

**ProductDB (used by UI & CLI):**
- Collection: `products`
  - Fields: `code` (unique), `name`, `category`, `isbn`, `status`, `borrowedBy`, `createdDate`, `lastUpdated`

**LibraryDB (used by service layer):**
- Collection: `users`
  - Fields: `id`, `username` (unique), `password`, `role`, `studentNumber` (if student)
- Collection: `transactions`
  - Fields: `action` (borrow/return), `title`, `isbn`, `username`, `timestamp`

### Key Classes

| Class | Purpose |
|-------|---------|
| [`Main`](src/main/java/com/example/demo/Main.java) | CLI entry point with menu loop |
| [`AppLauncher`](src/main/java/com/example/demo/AppLauncher.java) | JavaFX launcher + user seeding |
| [`MainController`](src/main/java/com/example/demo/MainController.java) | UI event handlers & role logic |
| [`LibraryManagementSystem`](src/main/java/service/LibraryManagementSystem.java) | Business logic (login, borrow, return, user CRUD) |
| [`Product`](src/main/java/com/example/demo/Product.java) | Product data model |
| [`User` (abstract)](src/main/java/model/User.java) | Base user class |
| [`Librarian`](src/main/java/model/Librarian.java) | Librarian user role |
| [`Reader`](src/main/java/model/Reader.java) | Reader user role |
| [`Admin`](src/main/java/model/Admin.java) | Admin user role |
| [`Student`](src/main/java/model/Student.java) | Student user role with studentNumber |

---

## Configuration

### Connection Defaults

**UI (JavaFX):**
- Pre-filled on startup in [`MainController.initialize()`](src/main/java/com/example/demo/MainController.java):
  - URI: `mongodb://localhost:27017`
  - DB: `ProductDB`
  - Collection: `products`

**Service (Library/Auth):**
- Defined in [`service.MongoDBConnection`](src/main/java/service/MongoDBConnection.java):
  - Default DB: `LibraryDB`

**CLI (Console):**
- Defined in [`MyConstants`](src/main/java/com/example/demo/MyConstants.java):
  - Default DB: `ProductDB`

### Modifying Defaults

1. **Change UI defaults:**
   - Edit [`MainController.initialize()`](src/main/java/com/example/demo/MainController.java) lines with `uriField.setText()`, `dbField.setText()`, `collectionField.setText()`

2. **Change Service/Auth DB:**
   - Edit [`service.MongoDBConnection`](src/main/java/service/MongoDBConnection.java) constructor default DB name

3. **Change CLI defaults:**
   - Edit [`MyConstants.DB_NAME`](src/main/java/com/example/demo/MyConstants.java)

---

## Troubleshooting

### MongoDB Connection Fails
- **Error:** `Unable to connect to localhost:27017`
- **Solution:** Ensure MongoDB is running
  ```sh
  mongod
  ```

### SLF4J Warnings
- **Warning:** `SLF4J not found on the classpath`
- **Solution:** Already included in `pom.xml` (slf4j-simple). If not, add:
  ```xml
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.9</version>
  </dependency>
  ```

### JavaFX Module Warnings
- **Warning:** `Unsupported JavaFX configuration: classes loaded from 'unnamed module'`
- **Solution:** Use `mvn javafx:run` or ensure proper JavaFX module setup

### Login Fails / Default Users Not Created
- **Issue:** Cannot login with admin/lib/read
- **Solution:** 
  - Ensure [`AppLauncher`](src/main/java/com/example/demo/AppLauncher.java) seeds users (runs on GUI startup)
  - Check LibraryDB `users` collection in MongoDB
  - Manually seed:
    ```java
    MongoDBConnection conn = new service.MongoDBConnection();
    LibraryManagementSystem lms = new LibraryManagementSystem(conn.getDatabase());
    lms.addUser(new Admin("u-admin", "admin", "123"));
    conn.close();
    ```

### Borrow/Return Shows "No Available Book"
- **Issue:** Book visible in table but borrow fails
- **Solution:** 
  - Ensure status is exactly `"Available"` (case-sensitive)
  - Verify UI is connected to correct DB/collection
  - Check [`LibraryManagementSystem.borrowBook()`](src/main/java/service/LibraryManagementSystem.java) uses correct collection name

---

## Development Notes

### Adding New Features

1. **New User Role:**
   - Create class extending [`User`](src/main/java/model/User.java) in `model/`
   - Update [`LibraryManagementSystem.docToUser()`](src/main/java/service/LibraryManagementSystem.java)
   - Update role visibility in [`MainController.applyRoleVisibility()`](src/main/java/com/example/demo/MainController.java)

2. **New Menu Item (CLI):**
   - Add case in [`Main.main()`](src/main/java/com/example/demo/Main.java) switch statement
   - Implement handler method

3. **New Button (JavaFX):**
   - Add `@FXML Button` field in [`MainController`](src/main/java/com/example/demo/MainController.java)
   - Add button to [`MainView.fxml`](src/main/resources/MainView.fxml) with `fx:id` and `onAction`
   - Implement handler method

---

## Build Information

- **Java Version:** 21
- **Build Tool:** Maven 3.6+
- **Dependencies:**
  - MongoDB Driver Sync 4.11.0
  - JavaFX 21.0.0
  - SLF4J Simple 2.0.9

**Build command:**
```sh
mvn clean compile
```

---

## License

Open source. Use and modify as needed.
