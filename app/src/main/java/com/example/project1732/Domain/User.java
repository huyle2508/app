package com.example.project1732.Domain;

public class User {
    private String email;
    private String displayName;
    private long createdAt;
    private String address; // Thêm địa chỉ
    private String phone;   // Thêm số điện thoại

    // Constructor mặc định (CẦN THIẾT cho Firebase)
    public User() {
    }

    // Constructor đầy đủ (hoặc bạn có thể dùng setters)
    public User(String email, String displayName, long createdAt, String address, String phone) {
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.address = address;
        this.phone = phone;
    }

    // --- Getters ---
    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getAddress() { // Getter cho address
        return address;
    }

    public String getPhone() { // Getter cho phone
        return phone;
    }

    // --- Setters ---
    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setAddress(String address) { // Setter cho address
        this.address = address;
    }

    public void setPhone(String phone) { // Setter cho phone
        this.phone = phone;
    }
}