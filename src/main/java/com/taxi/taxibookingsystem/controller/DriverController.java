//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.taxi.taxibookingsystem.controller;

import com.taxi.taxibookingsystem.service.FileService;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DriverController {
    private final FileService fileService = new FileService();
    private final double RATE_PER_KM = (double)80.0F;

    @GetMapping({"/driver/dashboard"})
    public String showDriverDashboard(HttpSession session, Model model) {
        if (session.getAttribute("loggedInDriver") == null) {
            return "redirect:/driver/login";
        } else {
            List<String> allBookings = this.fileService.readFromFile("bookings.txt");
            model.addAttribute("bookings", allBookings);
            return "driver_dashboard";
        }
    }

    @PostMapping({"/driver/confirm"})
    public String confirmBooking(@RequestParam String bookingId, HttpSession session) {
        String driverId = (String)session.getAttribute("loggedInDriver");
        if (driverId == null) {
            return "redirect:/driver/login";
        } else {
            this.updateBookingInFile(bookingId, "Confirmed", driverId, "0", "0");
            return "redirect:/driver/dashboard";
        }
    }

    @PostMapping({"/driver/complete"})
    public String completeBooking(@RequestParam String bookingId, @RequestParam double distance, HttpSession session) {
        String driverId = (String)session.getAttribute("loggedInDriver");
        if (driverId == null) {
            return "redirect:/driver/login";
        } else {
            double totalPayment = distance * (double)80.0F;
            this.updateBookingInFile(bookingId, "Completed", driverId, String.valueOf(distance), String.valueOf(totalPayment));
            return "redirect:/driver/dashboard";
        }
    }

    private void updateBookingInFile(String bId, String status, String dId, String dist, String pay) {
        List<String> bookings = this.fileService.readFromFile("bookings.txt");
        List<String> updatedList = new ArrayList();

        for(String line : bookings) {
            if (!line.trim().isEmpty()) {
                String[] b = line.split(",");
                if (b[0].equals(bId)) {
                    updatedList.add(b[0] + "," + b[1] + "," + b[2] + "," + b[3] + "," + status + "," + dId + "," + dist + "," + pay);
                } else {
                    updatedList.add(line);
                }
            }
        }

        this.saveAllBookings(updatedList);
    }

    private void saveAllBookings(List<String> data) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/resources/data/bookings.txt"))) {
            for(String line : data) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @GetMapping({"/apply-driver"})
    public String showDriverForm() {
        return "driver_apply";
    }
}
