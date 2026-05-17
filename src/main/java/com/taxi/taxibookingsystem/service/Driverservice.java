package com.taxi.taxibookingsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriverService {

    @Autowired
    private VehicleService vehicleService;

    public boolean isUserRegisteredDriver(String name) {
        File file = new File("drivers.txt");
        if (!file.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.lines().anyMatch(line -> line.contains("," + name + ","));
        } catch (IOException e) { return false; }
    }

    public String getDriverCabType(String name) {
        for (String line : vehicleService.getAllVehicles()) {
            if (line.toLowerCase().contains(name.toLowerCase())) {
                String lowerLine = line.toLowerCase();

                if (lowerLine.contains("premium") || lowerLine.contains("luxury")) return "Premium";

                if (lowerLine.contains("family")) return "Family";
                if (lowerLine.contains("comfort")) return "Comfort";

                if (lowerLine.contains("fast") || lowerLine.contains("cheap")) return "Fast & Cheap";

                if (lowerLine.contains("economy")) return "Economy";
            }
        }
        return "Economy"; // Default fallback
    }

    public List<String> getAllDrivers() { return readFile("drivers.txt"); }
    public List<String> getPendingDrivers() { return readFile("pending_drivers.txt"); }

    public void applyDriver(String id, String name, String license, String plate, String model, String type) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("pending_drivers.txt", true))) {
            String email = name.toLowerCase().replace(" ", "") + "@taxi.com";
            bw.write(id + "," + name + "," + email + "," + license + "," + plate + "," + model + "," + type + ",Pending Application");
            bw.newLine();
        } catch (IOException e) { e.printStackTrace(); }

        String vehicleData = plate + " (" + model + ") - " + type + " - Available - Owned by " + name;
        vehicleService.addVehicle(vehicleData);
    }

    public void addDriver(String driverData) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("drivers.txt", true))) {
            bw.write(driverData); bw.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void processPendingDriver(String id, boolean isApproved) {
        File inputFile = new File("pending_drivers.txt");
        File tempFile = new File("pending_drivers_temp.txt");
        String approvedDriverData = null;

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] details = line.split(",");
                if (details[0].equals(id)) {
                    if (isApproved) {
                        approvedDriverData = details[0] + "," + details[1] + "," + details[2] + "," + details[3] + ",Available";
                    }
                } else {
                    bw.write(line); bw.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        replaceFile(tempFile, inputFile);

        if (isApproved && approvedDriverData != null) {
            addDriver(approvedDriverData);
        }
    }

    public void deleteDriver(String id) {
        deleteLineFromFile("drivers.txt", id + ",");
    }

    public void editDriver(String originalId, String id, String name, String email, String license, String status) {
        File inputFile = new File("drivers.txt");
        File tempFile = new File("drivers_temp.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(originalId + ",")) {
                    bw.write(id + "," + name + "," + email + "," + license + "," + status);
                } else {
                    bw.write(line);
                }
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
        replaceFile(tempFile, inputFile);
    }

    private List<String> readFile(String fileName) {
        List<String> list = new ArrayList<>();
        File file = new File(fileName);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) list.add(line);
            } catch (IOException e) { e.printStackTrace(); }
        }
        return list;
    }

    private void deleteLineFromFile(String fileName, String startsWith) {
        File inputFile = new File(fileName);
        File tempFile = new File("temp_" + fileName);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(startsWith)) {
                    bw.write(line); bw.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        replaceFile(tempFile, inputFile);
    }

    private void replaceFile(File tempFile, File inputFile) {
        try { Files.move(tempFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { if(inputFile.delete()) tempFile.renameTo(inputFile); }
    }
}