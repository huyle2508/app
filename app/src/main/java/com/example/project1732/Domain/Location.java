package com.example.project1732.Domain;

public class Location {
    private int Id;
    private String Loc;

    @Override
    public String toString() {
        return  Loc ;
    }

    public Location(int id, String loc) {
        Id = id;
        Loc = loc;
    }

    public Location() {
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getLoc() {
        return Loc;
    }

    public void setLoc(String loc) {
        Loc = loc;
    }
}
