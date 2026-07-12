package com.library.model;

public class Staff {
    private int staffId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role;

    public Staff() {}

    public Staff(int staffId, String username, String fullName, String role) {
        this.staffId = staffId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
