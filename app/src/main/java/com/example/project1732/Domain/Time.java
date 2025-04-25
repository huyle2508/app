package com.example.project1732.Domain;

public class Time {
    private int id;
    private String Value;

    public Time() {
    }

    @Override
    public String toString() {
        return Value ;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }
}
