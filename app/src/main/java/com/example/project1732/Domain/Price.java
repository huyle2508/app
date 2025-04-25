package com.example.project1732.Domain;

public class Price {
    private int id;
    private String Value;

    public Price() {
    }

    public Price(int id, String value) {
        this.id = id;
        Value = value;
    }

    @Override
    public String toString() {
        return  Value ;
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
