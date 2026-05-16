package com.taxi.taxibookingsystem.model;

public class Driver {
    private String id;
    private String name;
    private String email;
    private String licenseNumber;
    private String status;

    public Driver(String id, String name, String email, String licenseNumber, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.status = status;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setStatus(String status) { this.status = status; }

    // Formats for drivers.txt
    @Override
    public String toString() {
        return id + "," + name + "," + email + "," + licenseNumber + "," + status;
    }
}