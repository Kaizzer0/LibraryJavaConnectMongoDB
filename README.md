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

## Prerequisites

- **Java 21 JDK** installed
- **Maven 3.6+**
- **MongoDB server** running on `localhost:27017` (or specify custom URI)

### Verify Java 21
```sh
java -version
mvn -v

```

If not Java 21, install from [Eclipse Adoptium](https://adoptium.net/) and set `JAVA_HOME`.

## Quick Start

### 1. Build
```sh
mvn clean compile
```

### 2. Run Console (CLI)
```sh
mvn exec:java -Dexec.mainClass=com.example.demo.Main
```

**What it does:**
- Seeds default users (admin, lib, read) into `LibraryDB`
- Provides menu-driven product CRUD operations
- Uses `ProductDB` database for products

**Main file:** [`src/main/java/com/example/demo/Main.java`](src/main/java/com/example/demo/Main.java)

### 3. Run JavaFX GUI
```sh
mvn javafx:run
```

Or alternatively:
```sh
mvn exec:java -Dexec.mainClass=com.example.demo.AppLauncher
```

**What it does:**
- Launches JavaFX UI with connection fields pre-filled:
  - URI: `mongodb://localhost:27017`
  - DB: `ProductDB`
  - Collection: `products`
- Seeds default users into `LibraryDB` if not present
- Provides login and role-based UI (Admin, Librarian, Reader, Student)

**Main files:**
- Launcher: [`src/main/java/com/example/demo/AppLauncher.java`](src/main/java/com/example/demo/AppLauncher.java)
- JavaFX App: [`src/main/java/com/example/demo/App.java`](src/main/java/com/example/demo/App.java)
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

3. **Example: Insert a product**
   ```
   Choice: 1
   Enter code: P001
   Enter name: Java Programming Book
   Enter price: 29.99
   Inserted: Product{code='P001', name='Java Programming Book', price=29.99}
   ```

### JavaFX GUI Mode

1. **Start the app:**
   ```sh
   mvn javafx:run
   ```

2. **Login (Required for role-based features):**
   - Click **Login** button
   - Default credentials:
     - **Admin**: `admin` / `123`
     - **Librarian**: `lib` / `123`
     - **Reader**: `read` / `123`

3. **Connect to MongoDB:**
   - Fields are pre-filled with defaults
   - Click **Connect** to load data

4. **Role-Based Features:**

   **Librarian (login as "lib"):**
   - **Add Book**: Click **Add** → Fill: Code, Name, Category, ISBN, Status → OK
   - **Update Book**: Select row → Click **Update** → Modify fields → OK
   - **Delete Book**: Select row → Click **Delete** → Confirm

   **Reader/Student (login as "read"):**
   - **Borrow Book**: Click **Borrow** → Enter exact book name → Confirm
   - **Return Book**: Click **Return** → Enter exact book name → Confirm
   - Status changes from "Available" to "Borrowed" in table

   **Admin (login as "admin"):**
   - Click **User Mgmt (Admin)**
   - **Create User**: Click **Create User** → Select role → Set username/password → OK
   - **Delete User**: Select user → Click **Delete Selected** → Confirm (cannot delete self)
   - **Refresh**: Reload user list

5. **Search:**
   - Type in search field (searches by code or name)
   - Click **Refresh** to reset filter and reload all data

---

## Architecture

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   ├── com/example/demo/
│   │   │   ├── Main.java              (CLI entry point)
│   │   │   ├── App.java               (JavaFX Application)
│   │   │   ├── AppLauncher.java       (Launcher with user seeding)
│   │   │   ├── MainController.java    (JavaFX controller)
│   │   │   ├── Product.java           (Product model)
│   │   │   ├── MongoDBConnection.java (App DB connector - ProductDB)
│   │   │   └── MyConstants.java       (App constants)
│   │   ├── model/
│   │   │   ├── User.java              (Abstract user class)
│   │   │   ├── Admin.java
│   │   │   ├── Librarian.java
│   │   │   ├── Reader.java
│   │   │   ├── Student.java
│   │   │   ├── Book.java              (Abstract book class)
│   │   │   ├── EBook.java
│   │   │   ├── PrintedBook.java
│   │   │   └── PhysicalBook.java
│   │   └── service/
│   │       ├── MongoDBConnection.java      (Service connector - LibraryDB)
│   │       └── LibraryManagementSystem.java (Business logic)
│   └── resources/
│       └── MainView.fxml              (JavaFX UI layout)
└── test/
    └── ... (tests)
```

### Database Schema

**ProductDB** (used by UI & CLI for products):
- **Collection**: `products`
  - Fields: `code` (unique), `name`, `category`, `isbn`, `status`, `borrowedBy`, `createdDate`, `lastUpdated`

**LibraryDB** (used by service layer for users):
- **Collection**: `users`
  - Fields: `id`, `username` (unique), `password`, `role`, `studentNumber` (if student)
- **Collection**: `transactions`
  - Fields: `action` (borrow/return), `title`, `isbn`, `username`, `timestamp`

### Key Classes & Responsibilities

| Class | Purpose | File |
|-------|---------|------|
| **Main** | CLI entry point with menu loop | [`src/main/java/com/example/demo/Main.java`](src/main/java/com/example/demo/Main.java) |
| **AppLauncher** | JavaFX launcher + user seeding | [`src/main/java/com/example/demo/AppLauncher.java`](src/main/java/com/example/demo/AppLauncher.java) |
| **App** | JavaFX Application class | [`src/main/java/com/example/demo/App.java`](src/main/java/com/example/demo/App.java) |
| **MainController** | UI event handlers & role logic | [`src/main/java/com/example/demo/MainController.java`](src/main/java/com/example/demo/MainController.java) |
| **LibraryManagementSystem** | Business logic (login, borrow, return, user CRUD) | [`src/main/java/service/LibraryManagementSystem.java`](src/main/java/service/LibraryManagementSystem.java) |
| **Product** | Product data model | [`src/main/java/com/example/demo/Product.java`](src/main/java/com/example/demo/Product.java) |
| **User** (abstract) | Base user class | [`src/main/java/model/User.java`](src/main/java/model/User.java) |
| **MongoDBConnection** (com.example.demo) | App DB connector (ProductDB) | [`src/main/java/com/example/demo/MongoDBConnection.java`](src/main/java/com/example/demo/MongoDBConnection.java) |
| **MongoDBConnection** (service) | Service DB connector (LibraryDB) | [`src/main/java/service/MongoDBConnection.java`](src/main/java/service/MongoDBConnection.java) |

---

## Configuration

### Connection Defaults

**UI (JavaFX)** - Pre-filled in [`MainController.initialize()`](src/main/java/com/example/demo/MainController.java):
- URI: `mongodb://localhost:27017`
- DB: `ProductDB`
- Collection: `products`

**Service Layer (Library/Auth)** - Defined in [`service.MongoDBConnection`](src/main/java/service/MongoDBConnection.java):
- Default DB: `LibraryDB`

**CLI (Console)** - Defined in [`MyConstants`](src/main/java/com/example/demo/MyConstants.java):
- Default DB: `ProductDB`

### Modifying Defaults

1. **Change UI connection defaults:**
   - Edit [`MainController.initialize()`](src/main/java/com/example/demo/MainController.java) lines with:
     ```java
     uriField.setText("mongodb://localhost:27017");
     dbField.setText("ProductDB");
     collectionField.setText("products");
     ```

2. **Change Service/Auth database:**
   - Edit [`service.MongoDBConnection`](src/main/java/service/MongoDBConnection.java) constructor:
     ```java
     this.database = client.getDatabase("LibraryDB"); // Change to desired DB
     ```

3. **Change CLI defaults:**
   - Edit [`MyConstants`](src/main/java/com/example/demo/MyConstants.java):
     ```java
     public static final String DB_NAME = "ProductDB";
     ```

---

## Troubleshooting

### MongoDB Connection Fails
**Error:** `Unable to connect to localhost:27017`

**Solution:** Ensure MongoDB is running
```sh
mongod
# Or use MongoDB Compass/Atlas
```

### SLF4J Warnings
**Warning:** `SLF4J not found on the classpath`

**Solution:** Already included in [`pom.xml`](pom.xml) (`slf4j-simple`). No action needed.

### JavaFX Module Warnings
**Warning:** `Unsupported JavaFX configuration: classes loaded from 'unnamed module'`

**Solution:** Use `mvn javafx:run` (recommended) or ensure proper module configuration.

### Login Fails / Default Users Not Created
**Issue:** Cannot login with admin/lib/read

**Solution:**
1. Ensure [`AppLauncher`](src/main/java/com/example/demo/AppLauncher.java) runs (seeds users on startup)
2. Check `LibraryDB.users` collection in MongoDB:
   ```sh
   mongosh
   use LibraryDB
   db.users.find()
   ```
3. If empty, run:
   ```sh
   mvn exec:java -Dexec.mainClass=com.example.demo.AppLauncher
   ```

### Borrow/Return Shows "No Available Book"
**Issue:** Book visible in table but borrow fails

**Solution:**
- Verify status is exactly `"Available"` (case-sensitive)
- Ensure UI is connected to correct DB (`ProductDB`)
- Check [`LibraryManagementSystem`](src/main/java/service/LibraryManagementSystem.java) uses collection name `"products"` (line 28)

### Java Version Mismatch
**Error:** `Unsupported class file major version 65`

**Solution:**
1. Verify Java 21:
   ```sh
   java -version
   mvn -v
   ```
2. Install Java 21 and set `JAVA_HOME`
3. Clean rebuild:
   ```sh
   mvn clean compile
   ```

---

## Development Notes

### Adding New Features

**1. New User Role:**
- Create class extending [`User`](src/main/java/model/User.java) in `model/`
- Update [`LibraryManagementSystem.docToUser()`](src/main/java/service/LibraryManagementSystem.java)
- Update [`MainController.applyRoleVisibility()`](src/main/java/com/example/demo/MainController.java)

**2. New Menu Item (CLI):**
- Add case in [`Main.main()`](src/main/java/com/example/demo/Main.java) switch statement
- Implement handler method

**3. New Button (JavaFX):**
- Add `@FXML Button` field in [`MainController`](src/main/java/com/example/demo/MainController.java)
- Add button to [`MainView.fxml`](src/main/resources/MainView.fxml) with `fx:id` and `onAction`
- Implement handler method

### MongoDB Schema Management

**Create unique index on product code:**
```javascript
use ProductDB
db.products.createIndex({ code: 1 }, { unique: true })
```

**View collections:**
```javascript
use ProductDB
db.products.find().pretty()

use LibraryDB
db.users.find().pretty()
db.transactions.find().pretty()
```

---

## Build Information

- **Java Version:** 21
- **Build Tool:** Maven 3.6+
- **Dependencies:**
  - MongoDB Driver Sync 4.11.0
  - JavaFX 21.0.2
  - SLF4J Simple 2.0.9

**Build command:**
```sh
mvn clean compile
```

**Package (create JAR):**
```sh
mvn clean package
```

---

## Project Files Reference

| File | Description |
|------|-------------|
| [`pom.xml`](pom.xml) | Maven project configuration (Java 21, dependencies) |
| [`src/main/java/com/example/demo/Main.java`](src/main/java/com/example/demo/Main.java) | CLI entry point |
| [`src/main/java/com/example/demo/AppLauncher.java`](src/main/java/com/example/demo/AppLauncher.java) | JavaFX launcher with user seeding |
| [`src/main/java/com/example/demo/App.java`](src/main/java/com/example/demo/App.java) | JavaFX Application class |
| [`src/main/java/com/example/demo/MainController.java`](src/main/java/com/example/demo/MainController.java) | JavaFX controller |
| [`src/main/resources/MainView.fxml`](src/main/resources/MainView.fxml) | JavaFX UI layout |
| [`src/main/java/service/LibraryManagementSystem.java`](src/main/java/service/LibraryManagementSystem.java) | Business logic service |
| [`src/main/java/model/User.java`](src/main/java/model/User.java) | Abstract user model |
| [`.gitignore`](.gitignore) | Git ignore patterns |

---

## License

Open source. Use and modify as needed.
