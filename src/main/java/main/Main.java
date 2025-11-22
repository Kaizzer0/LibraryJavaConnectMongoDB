package main;

import model.*;
import service.LibraryManagementSystem;
import service.MongoDBConnection;

import java.util.Scanner;

public class Main {
    private static LibraryManagementSystem lms;
    private static Scanner scanner;

    public static void main(String[] args) {
        // 1. Initialize
        final MongoDBConnection mongo = new MongoDBConnection();
        lms = new LibraryManagementSystem(mongo.getDatabase());
        scanner = new Scanner(System.in);

        // 2. Register safe shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Closing MongoDB connection...");
            try { mongo.close(); } catch (Exception ignored) {}
            try { scanner.close(); } catch (Exception ignored) {}
        }));

        // 3. SEED SAMPLE DATA (Admin: admin/123)
        // Added this line to fix "never used" error and create admin account
        seedData(); 

        System.out.println("=== LIBRARY MANAGEMENT SYSTEM (MONGODB) ===");

        // 4. Main program loop
        while (true) {
            try {
                System.out.println("\n--- LOGIN ---");
                System.out.print("Username: ");
                String username = scanner.nextLine();

                // Allow typing "exit" to quit immediately at login screen
                if (username.equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    System.exit(0);
                }

                System.out.print("Password: ");
                String password = scanner.nextLine();

                User currentUser = lms.login(username, password);

                if (currentUser == null) {
                    System.out.println("!!! Invalid username or password.");
                } else {
                    System.out.println(">>> Welcome, " + currentUser.getRole() + " " + currentUser.getUsername());
                    // Redirect to appropriate menu based on Role
                    handleUserMenu(currentUser);
                }

            } catch (Exception e) {
                System.out.println("!!! System error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Route menu based on user role
    private static void handleUserMenu(User user) {
        boolean isLoggedIn = true;
        while (isLoggedIn) {
            String role = user.getRole().toLowerCase();
            switch (role) {
                case "admin":
                    isLoggedIn = showAdminMenu((Admin) user);
                    break;
                case "librarian":
                    isLoggedIn = showLibrarianMenu((Librarian) user);
                    break;
                case "reader":
                case "student":
                    isLoggedIn = showReaderMenu(user);
                    break;
                default:
                    System.out.println("Invalid Role!");
                    isLoggedIn = false;
            }
        }
    }

    // --- ADMIN MENU ---
    private static boolean showAdminMenu(Admin admin) {
        System.out.println("\n--- ADMIN MENU ---");
        System.out.println("1. Add New User");
        System.out.println("2. Remove User");
        System.out.println("3. Logout");
        System.out.print("Select: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                createNewUserUI();
                return true;
            case "2":
                System.out.print("Enter username to remove: ");
                String uName = scanner.nextLine();
                boolean removed = lms.removeUser(uName);
                System.out.println(removed ? "User removed." : "User not found.");
                return true;
            case "3":
                return false;
            default:
                System.out.println("Invalid selection!");
                return true;
        }
    }

    // UI to create new User
    private static void createNewUserUI() {
        System.out.println("Select User Type: 1.Admin  2.Librarian  3.Reader  4.Student");
        String type = scanner.nextLine();
        System.out.print("Username: "); String u = scanner.nextLine();
        System.out.print("Password: "); String p = scanner.nextLine();

        User newUser = null;
        if ("1".equals(type)) newUser = new Admin(null, u, p); // ID null so DB auto-generates
        else if ("2".equals(type)) newUser = new Librarian(null, u, p);
        else if ("3".equals(type)) newUser = new Reader(null, u, p);
        else if ("4".equals(type)) {
            System.out.print("Student ID Number: ");
            String stuCode = scanner.nextLine();
            try {
                lms.addStudent(null, u, p, stuCode); // Use specialized method for Student
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
            return;
        }

        if (newUser != null) {
            try {
                lms.addUser(newUser);
                System.out.println("User added: " + newUser.getUsername());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid user type.");
        }
    }

    // --- LIBRARIAN MENU ---
    private static boolean showLibrarianMenu(Librarian lib) {
        System.out.println("\n--- LIBRARIAN MENU ---");
        System.out.println("1. Add New Book");
        System.out.println("2. List All Books");
        System.out.println("3. Logout");
        System.out.print("Select: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                createBookUI();
                return true;
            case "2":
                lms.listBooks();
                return true;
            case "3":
                return false;
            default:
                System.out.println("Invalid selection!");
                return true;
        }
    }

    private static void createBookUI() {
        System.out.println("Book Type: 1.Printed Book  2.EBook");
        String type = scanner.nextLine();
        System.out.print("Title: "); String title = scanner.nextLine();
        System.out.print("Author: "); String author = scanner.nextLine();
        System.out.print("ISBN: "); String isbn = scanner.nextLine();

        Book newBook = null;
        if ("1".equals(type)) {
            System.out.print("Number of Copies: ");
            int copies = Integer.parseInt(scanner.nextLine());
            newBook = new PrintedBook(isbn, title, author, copies, copies > 0);
        } else if ("2".equals(type)) {
            System.out.print("Download URL or format: ");
            String url = scanner.nextLine();
            newBook = new EBook(isbn, title, author, url);
        }

        if (newBook != null) {
            lms.addBook(newBook);
            System.out.println("Added book: " + newBook);
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    // --- READER MENU ---
    private static boolean showReaderMenu(User user) {
        System.out.println("\n--- READER MENU (" + user.getUsername() + ") ---");
        System.out.println("1. Borrow Book");
        System.out.println("2. Return Book");
        System.out.println("3. View Available Books");
        System.out.println("4. Logout");
        System.out.print("Select: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                System.out.print("Enter ISBN to borrow: ");
                String isbn = scanner.nextLine();
                boolean got = lms.borrowBook(isbn, user.getUsername());
                System.out.println(got ? "Borrow successful." : "Borrow failed.");
                return true;
            case "2":
                System.out.print("Enter ISBN to return: ");
                String returnIsbn = scanner.nextLine();
                boolean ret = lms.returnBook(returnIsbn, user.getUsername());
                System.out.println(ret ? "Return successful." : "Return failed.");
                return true;
            case "3":
                System.out.println("Books in Library:");
                lms.listBooks();
                return true;
            case "4":
                return false;
            default:
                System.out.println("Invalid selection!");
                return true;
            }
    }

    // This method is now used!
    private static void seedData() {
        try {
            // System.out.println("Checking and seeding data..."); 
            // (Uncomment above line to see detailed logs)
            
            if (lms.findUserByUsername("admin") == null) {
                lms.addUser(new Admin(null, "admin", "123"));
                System.out.println("- Created Admin: admin/123");
            }
            // Only add sample book if none exist (to avoid duplicates)
            if (lms.listAllBooks().isEmpty()) {
                 lms.addBook(new PrintedBook("VN-001", "Java Core", "G.Gosling", 500, true));
            }
        } catch (Exception e) {
            // Ignore error
        }
    }
}

