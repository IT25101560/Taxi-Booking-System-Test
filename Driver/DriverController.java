package com.taxi.taxibookingsystem.controller;

import com.taxi.taxibookingsystem.service.BookingService;
import com.taxi.taxibookingsystem.service.DriverService;
import com.taxi.taxibookingsystem.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/driver-portal")
    public String driverPortal(HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        if (username == null || !driverService.isUserRegisteredDriver(username)) return "redirect:/home";

        String driverCabType = driverService.getDriverCabType(username);
        model.addAttribute("myCabType", driverCabType);

        List<String> allBookings = bookingService.readAllBookings();

        model.addAttribute("availableJobs", allBookings.stream()
                .filter(b -> b.contains(",") && b.substring(b.lastIndexOf(",") + 1).trim().equals("PENDING") && b.contains(driverCabType))
                .collect(Collectors.toList()));

        model.addAttribute("ongoingJobs", allBookings.stream()
                .filter(b -> b.contains(",") && b.substring(b.lastIndexOf(",") + 1).trim().equals("ONGOING by " + username))
                .collect(Collectors.toList()));

        model.addAttribute("driverHistory", allBookings.stream()
                .filter(b -> b.contains(",") && b.substring(b.lastIndexOf(",") + 1).trim().startsWith("COMPLETED by " + username))
                .collect(Collectors.toList()));

        return "driver-portal";
    }

    @GetMapping("/drivers")
    public String manageDrivers(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/home";

        List<String> rawDrivers = driverService.getAllDrivers();
        List<String> allBookings = bookingService.readAllBookings();
        List<String> displayDrivers = new ArrayList<>();

        for (String driver : rawDrivers) {
            String[] parts = driver.split(",");
            if (parts.length >= 5) {
                String driverName = parts[1].trim();

                boolean isBusy = allBookings.stream().anyMatch(b -> {
                    if (!b.contains(",")) return false;
                    String status = b.substring(b.lastIndexOf(",") + 1).trim();
                    return status.equals("ONGOING by " + driverName);
                });

                if (isBusy) {
                    displayDrivers.add(parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + ",Busy");
                } else {
                    displayDrivers.add(driver);
                }
            } else {
                displayDrivers.add(driver);
            }
        }

        model.addAttribute("drivers", displayDrivers);
        model.addAttribute("pendingDrivers", driverService.getPendingDrivers());
        return "driver-management";
    }

    @PostMapping("/apply-driver")
    public String applyDriver(@RequestParam String id, @RequestParam String name, @RequestParam String license, @RequestParam String plate, @RequestParam String model, @RequestParam String type, HttpSession session) {
        if (session.getAttribute("role") == null) return "redirect:/login";
        driverService.applyDriver(id, name, license, plate, model, type);
        return "redirect:/home?application=success";
    }

    @PostMapping("/approve-driver")
    public String approveDriver(@RequestParam String id, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) driverService.processPendingDriver(id, true);
        return "redirect:/drivers";
    }

    @PostMapping("/reject-driver")
    public String rejectDriver(@RequestParam String id, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) driverService.processPendingDriver(id, false);
        return "redirect:/drivers";
    }

    @PostMapping("/delete-driver")
    public String deleteDriver(@RequestParam String id, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) driverService.deleteDriver(id);
        return "redirect:/drivers";
    }

    @PostMapping("/admin-add-driver")
    public String adminAddDriver(@RequestParam String id, @RequestParam String name, @RequestParam String email, @RequestParam String license, @RequestParam String plate, @RequestParam String model, @RequestParam String type, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) {
            driverService.addDriver(id + "," + name + "," + email + "," + license + ",Available");
            vehicleService.addVehicle(plate + " (" + model + ") - " + type + " - Available - Owned by " + name);
        }
        return "redirect:/drivers";
    }

    @PostMapping("/edit-driver")
    public String editDriver(@RequestParam String originalId, @RequestParam String id, @RequestParam String name, @RequestParam String email, @RequestParam String license, @RequestParam String status, HttpSession session) {
        if ("ADMIN".equals(session.getAttribute("role"))) driverService.editDriver(originalId, id, name, email, license, status);
        return "redirect:/drivers";
    }
}