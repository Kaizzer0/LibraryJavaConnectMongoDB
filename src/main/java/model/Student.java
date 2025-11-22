package model;

import org.bson.Document;

public class Student extends User {
    private final String studentNumber;

    public Student(String id, String username, String password, String studentNumber) {
        super(id, username, password, "student");
        this.studentNumber = studentNumber;
    }

    public String getStudentNumber() { return studentNumber; }

    @Override
    public String toString() {
        return "Student{" + "id='" + getId() + "', username='" + getUsername() + "', studentNumber='" + studentNumber + "'}";
    }

    @Override
    public Document toDocument() {
        Document d = super.toDocument();
        if (studentNumber != null) {
            d.append("studentNumber", studentNumber);
        }
        return d;
    }
}