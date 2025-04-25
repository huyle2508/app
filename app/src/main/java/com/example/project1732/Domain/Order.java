package com.example.project1732.Domain;

import java.util.ArrayList;

public class Order {
    private String orderId;
    private String userId;
    private long timestamp;
    private double totalAmount;
    private String status;
    private ArrayList<Foods> items;
    private String address;
    private String phone;
    private String paymentMethod; // <-- THÊM TRƯỜNG NÀY

    // Constructor mặc định cho Firebase
    public Order() {
    }

    // Constructor chính cập nhật
    public Order(String orderId, String userId, long timestamp, double totalAmount, String status,
                 ArrayList<Foods> items, String address, String phone, String paymentMethod) { // <-- Thêm tham số
        this.orderId = orderId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        this.address = address;
        this.phone = phone;
        this.paymentMethod = paymentMethod; // <-- Gán giá trị
    }

    // --- Getters ---
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public long getTimestamp() { return timestamp; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public ArrayList<Foods> getItems() { return (items != null) ? new ArrayList<>(items) : new ArrayList<>(); }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getPaymentMethod() { return paymentMethod; } // <-- Getter cho paymentMethod

    // --- Setters (Tùy chọn) ---
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setItems(ArrayList<Foods> items) { this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>(); }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; } // <-- Setter cho paymentMethod
}