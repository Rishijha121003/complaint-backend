package com.college.complaint.controller;

import com.college.complaint.dto.ComplaintRequest; // Ensure this DTO exists
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.repository.UserRepository;
import com.college.complaint.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Hum API bana rahe hain isliye @RestController use karenge
@RequestMapping("/api/student")
public class StudentController {

    private final UserRepository userRepository;
    private final ComplaintService complaintService;

    public StudentController(UserRepository userRepository, ComplaintService complaintService) {
        this.userRepository = userRepository;
        this.complaintService = complaintService;
    }

    // 1. Complaint Raise karne ke liye (Ye missing tha!)
    @PostMapping(value = "/raise", consumes = { "multipart/form-data" })
    public ResponseEntity<?> raiseComplaint(@ModelAttribute ComplaintRequest request,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestHeader(value = "User-Email", required = true) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(401).body("Error: User-Email header is missing. Please login again.");
        }

        try {
            if (image != null && !image.isEmpty()) {
                String fileName = java.util.UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", fileName);
                java.nio.file.Files.copy(image.getInputStream(), filePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                request.setImageUrl("/uploads/" + fileName);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: Failed to upload image.");
        }

        Complaint savedComplaint = complaintService.saveComplaint(request, email);
        return ResponseEntity.ok(savedComplaint);
    }

    // 2. Student ki saari complaints fetch karne ke liye
    @GetMapping("/my-complaints")
    public List<Complaint> getMyComplaints(@RequestHeader(value = "User-Email", required = true) String email) {
        if (email == null)
            email = "student@test.com";
        User student = userRepository.findByEmail(email); 
        return complaintService.getComplaintsByStudent(student);
    }

    // 3. Student close karne ke liye
    @PatchMapping("/close/{id}")
    public ResponseEntity<?> closeComplaint(@PathVariable Long id,
            @RequestHeader(value = "User-Email", required = true) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(401).body("Error: User-Email header is missing. Please login again.");
        }
        try {
            User student = userRepository.findByEmail(email);
            Complaint closedComplaint = complaintService.closeComplaint(id, student);
            return ResponseEntity.ok(closedComplaint);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
}