package com.taxi.taxibookingsystem.controller;

import com.taxi.taxibookingsystem.service.BookingService;
import com.taxi.taxibookingsystem.service.DriverService;
import com.taxi.taxibookingsystem.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private DriverService driverService;

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/home")
    public String index(HttpSession session, Model model) {
        if (session.getAttribute("role") == null) return "redirect:/login";
        if ("ADMIN".equals(session.getAttribute("role"))) return "redirect:/admin";

        model.addAttribute("isDriver", driverService.isUserRegisteredDriver((String) session.getAttribute("user")));
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "index";
    }

    @PostMapping("/book")
    public String book(@RequestParam("pickupLocation") String pickup, @RequestParam("dropoffLocation") String dropoff, @RequestParam("cabType") String cabType, HttpSession session) {
        if (session.getAttribute("role") == null) return "redirect:/login";
        String customerName = (String) session.getAttribute("user");
        bookingService.createBooking(customerName == null ? "Guest" : customerName, pickup, dropoff, cabType);
        return "redirect:/view";
    }

    @GetMapping("/view")
    public String view(HttpSession session, Model model) {
        String currentUser = (String) session.getAttribute("user");
        String role = (String) session.getAttribute("role");
        if (currentUser == null) return "redirect:/login";

        List<String> allBookings = bookingService.readAllBookings();

        if ("ADMIN".equals(role)) {
            model.addAttribute("bookings", allBookings);
            model.addAttribute("isAdminView", true);
        } else {
            List<String> userBookings = allBookings.stream().filter(b -> b.startsWith(currentUser + " (")).collect(Collectors.toList());

            model.addAttribute("pendingBookings", filterStatus(userBookings, "PENDING", true));
            model.addAttribute("ongoingBookings", filterStatus(userBookings, "ONGOING", false));
            model.addAttribute("completedBookings", filterStatus(userBookings, "COMPLETED", false));
            model.addAttribute("completedBookings", filterStatus(userBookings, "DECLINED", false)); // Includes declined
            model.addAttribute("isAdminView", false);
        }
        return "view-bookings";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("bookingDetail") String bookingDetail, HttpSession session) {
        if (session.getAttribute("user") != null) bookingService.deleteSpecificBooking(bookingDetail);
        return "redirect:/view";
    }

    @PostMapping("/accept-booking")
    public String acceptBooking(@RequestParam String bookingDetail, HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username != null) bookingService.updateBookingFileStatus(bookingDetail, "ONGOING by " + username);
        return "redirect:/driver-portal";
    }

    @PostMapping("/decline-booking")
    public String declineBooking(@RequestParam String bookingDetail, HttpSession session) {
        if (session.getAttribute("user") != null) bookingService.updateBookingFileStatus(bookingDetail, "DECLINED");
        return "redirect:/driver-portal";
    }

    @PostMapping("/finish-booking")
    public String finishBooking(@RequestParam String bookingDetail, @RequestParam double actualDistance, HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username != null && bookingDetail.split(",").length >= 4) {
            double finalFare = bookingService.calculateFare(bookingDetail.split(",")[1], actualDistance);
            bookingService.finishTripAndUpdateFile(bookingDetail, "COMPLETED by " + username, actualDistance, finalFare);
        }
        return "redirect:/driver-portal";
    }

    private List<String> filterStatus(List<String> bookings, String target, boolean exactMatch) {
        return bookings.stream().filter(b -> {
            if (!b.contains(",")) return false;
            String status = b.substring(b.lastIndexOf(",") + 1).trim();
            return exactMatch ? status.equals(target) : status.startsWith(target);
        }).collect(Collectors.toList());
    }
}