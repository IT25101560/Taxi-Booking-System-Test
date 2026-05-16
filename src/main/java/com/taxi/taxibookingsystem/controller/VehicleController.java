package com.taxi.taxibookingsystem.controller;

import com.taxi.taxibookingsystem.service.BookingService;
import com.taxi.taxibookingsystem.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/register-vehicle")
    public String vehicleRegistrationPage(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/home";

        List<String> rawVehicles = vehicleService.getAllVehicles();
        List<String> allBookings = bookingService.readAllBookings();
        List<String> displayVehicles = new ArrayList<>();

        for (String vehicle : rawVehicles) {
            String[] parts = vehicle.split(" - ");
            if (parts.length >= 4) {

                String ownerName = parts[3].replace("Owned by", "").trim();

                boolean isBusy = allBookings.stream().anyMatch(b -> {
                    if (!b.contains(",")) return false;
                    String status = b.substring(b.lastIndexOf(",") + 1).trim();
                    return status.equals("ONGOING by " + ownerName);
                });

                if (isBusy) {
                    displayVehicles.add(parts[0] + " - " + parts[1] + " - Busy - " + parts[3]);
                } else {
                    displayVehicles.add(vehicle);
                }
            } else {
                displayVehicles.add(vehicle);
            }
        }

        model.addAttribute("vehicles", displayVehicles);
        return "vehicle-registration";
    }

    @PostMapping("/add-vehicle")
    public String addVehicle(@RequestParam String plate, @RequestParam String model, @RequestParam String type, @RequestParam String owner, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) {
            vehicleService.addVehicle(plate + " (" + model + ") - " + type + " - Available - Owned by " + owner);
        }
        return "redirect:/register-vehicle";
    }

    @PostMapping("/delete-vehicle")
    public String deleteVehicle(@RequestParam int id, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) vehicleService.deleteVehicle(id);
        return "redirect:/register-vehicle";
    }

    @PostMapping("/edit-vehicle")
    public String editVehicle(@RequestParam int id, @RequestParam String plate, @RequestParam String model, @RequestParam String type, @RequestParam String owner, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) {
            String updatedData = plate + " (" + model + ") - " + type + " - Available - Owned by " + owner;
            vehicleService.editVehicle(id, updatedData);
        }
        return "redirect:/register-vehicle";
    }
}