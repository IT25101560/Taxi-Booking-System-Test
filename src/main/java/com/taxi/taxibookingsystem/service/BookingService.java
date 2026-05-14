package com.taxi.taxibookingsystem.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    public void createBooking(String customerName, String pickup, String dropoff, String cabType) {
        String data = customerName + " (" + pickup + " to " + dropoff + ")," + cabType + ",TBD,TBD,PENDING";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("bookings.txt", true))) {
            bw.write(data); bw.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public List<String> readAllBookings() {
        List<String> bookings = new ArrayList<>();
        File file = new File("bookings.txt");
        if (!file.exists()) return bookings;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) bookings.add(line);
            }
        } catch (IOException e) { e.printStackTrace(); }
        return bookings;
    }

    // --- UPDATED PRICING LOGIC ---
    public double calculateFare(String cabType, double dist) {
        double fare;
        String typeLower = cabType.toLowerCase();

        if (typeLower.contains("luxury") || typeLower.contains("premium")) {
            fare = dist * 1000;
        } else if (typeLower.contains("family")) {
            fare = dist * 400;
        } else if (typeLower.contains("comfort")) {
            fare = dist * 300;
        } else if (typeLower.contains("economy")) {
            fare = dist * 100;
        } else {
            // Fallback for "Fast & Cheap", "FastCheap", etc.
            fare = dist * 70;
        }

        // Rounds the final fare to 2 decimal places (e.g. Rs. 1540.00)
        return Math.round(fare * 100.0) / 100.0;
    }

    public void updateBookingFileStatus(String oldLine, String newStatus) {
        processFile(oldLine, newStatus, false, 0, 0);
    }

    public void finishTripAndUpdateFile(String oldLine, String newStatus, double distance, double newFare) {
        processFile(oldLine, newStatus, true, distance, newFare);
    }

    public void deleteSpecificBooking(String targetLine) {
        processFile(targetLine, null, false, 0, 0); // null status implies deletion
    }

    // Safely reads and replaces the specific row in the bookings text file
    private void processFile(String targetLine, String newStatus, boolean alterMath, double dist, double fare) {
        if (targetLine == null || !targetLine.contains(",")) return;

        int lastCommaIdx = targetLine.lastIndexOf(",");
        String coreMatch = targetLine.substring(0, lastCommaIdx).trim();
        String targetStatusStr = targetLine.substring(lastCommaIdx + 1).trim();

        File inputFile = new File("bookings.txt");
        File tempFile = new File("bookings_temp.txt");
        boolean actionTaken = false;

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(",")) { bw.write(line); bw.newLine(); continue; }

                int lineLastComma = line.lastIndexOf(",");
                String lineCore = line.substring(0, lineLastComma).trim();
                String lineStatus = line.substring(lineLastComma + 1).trim();

                // It must match BOTH the core details AND the specific status
                if (!actionTaken && lineCore.equals(coreMatch) && lineStatus.equals(targetStatusStr)) {
                    actionTaken = true;
                    if (newStatus == null) continue; // Deletion

                    if (alterMath) {
                        String[] parts = lineCore.split(",");
                        if (parts.length >= 4) {
                            String newCore = parts[0] + "," + parts[1] + "," + dist + "," + fare;
                            bw.write(newCore + "," + newStatus);
                        } else {
                            bw.write(lineCore + "," + newStatus);
                        }
                    } else {
                        bw.write(lineCore + "," + newStatus);
                    }
                } else {
                    bw.write(line);
                }
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }

        try { Files.move(tempFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { if(inputFile.delete()) tempFile.renameTo(inputFile); }
    }
}