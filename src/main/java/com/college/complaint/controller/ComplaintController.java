package com.college.complaint.controller;

import com.college.complaint.dto.ComplaintRequest;
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.enums.ComplaintCategory;
import com.college.complaint.enums.ComplaintStatus;
import com.college.complaint.enums.Role;
import com.college.complaint.service.ComplaintService;
import com.college.complaint.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public ComplaintController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    // --- STUDENT ENDPOINTS ---

    // ✅ FIX: Path changed to /student/my-complaints to match frontend
    @GetMapping("/student/my-complaints")
    public ResponseEntity<List<Complaint>> getMyComplaints(
            @RequestHeader(value = "User-Email", required = false) String email) {
        if (email == null)
            email = "student@test.com";
        User student = userService.findByEmail(email);
        return ResponseEntity.ok(complaintService.getComplaintsByStudent(student));
    }

    @PostMapping("/raise")
    public ResponseEntity<?> raiseComplaint(@RequestBody ComplaintRequest request,
            @RequestHeader(value = "User-Email", required = false) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(401).body("Error: User-Email header is missing. Please login again.");
        }
        // ensure service method is called exactly like this
        Complaint saved = complaintService.saveComplaint(request, email);
        return ResponseEntity.ok(saved);
    }

    // --- STAFF ENDPOINTS ---

    @GetMapping("/staff/assigned")
    public ResponseEntity<?> getAssignedComplaints(
            @RequestHeader(value = "User-Email", required = false) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User-Email header is missing.");
        }
        User staff = userService.findByEmail(email);
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Staff not found");
        }
        return ResponseEntity.ok(complaintService.getComplaintsByStaff(staff));
    }

    @PatchMapping("/staff/update-status/{id}")
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
            @RequestParam String status, // String mein lein
            @RequestHeader(value = "User-Email", required = false) String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User-Email header is missing.");
            }
            User staff = userService.findByEmail(email);
            if (staff == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Staff not found");
            }

            // String ko Enum mein convert karein
            ComplaintStatus complaintStatus = ComplaintStatus.valueOf(status.toUpperCase());

            complaintService.updateStatus(id, complaintStatus, staff, null, null);
            return ResponseEntity.ok("Status updated to " + status + " ✅");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Status Value!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "/staff/resolve/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<String> resolveComplaint(@PathVariable Long id,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestHeader(value = "User-Email", required = false) String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User-Email header is missing.");
            }
            User staff = userService.findByEmail(email);
            if (staff == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Staff not found");
            }

            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                String fileName = java.util.UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", fileName);
                java.nio.file.Files.copy(image.getInputStream(), filePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                imageUrl = "/uploads/" + fileName;
            }

            complaintService.updateStatus(id, ComplaintStatus.RESOLVED, staff, remark, imageUrl);
            return ResponseEntity.ok("Complaint Marked as Resolved with Evidence! ✅");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Data!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- ADMIN ENDPOINTS ---

    // ✅ FIX: Consistency ke liye path me /admin/ add kiya gaya hai
    @GetMapping("/admin/staff-list")
    public ResponseEntity<List<User>> getAllStaff() {
        return ResponseEntity.ok(userService.findAllByRole(Role.STAFF));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Complaint>> getAllComplaintsForAdmin() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PostMapping("/admin/assign")
    public ResponseEntity<String> assignStaff(@RequestParam Long complaintId,
            @RequestParam Long staffId,
            @RequestHeader(value = "User-Email", required = false) String adminEmail) {
        try {
            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User-Email header is missing.");
            }
            User admin = userService.findByEmail(adminEmail);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Admin not found");
            }
            User staff = userService.findById(staffId);
            complaintService.assignStaff(complaintId, admin, staff);
            return ResponseEntity.ok("Staff assigned successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ✅ AUTO-ASSIGN ENDPOINT (Can be triggered by Admin or automatically on creation)
    @PostMapping("/admin/auto-assign")
    public ResponseEntity<String> autoAssignStaff(@RequestParam Long complaintId,
            @RequestHeader(value = "User-Email", required = false) String adminEmail) {
        try {
            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User-Email header is missing.");
            }
            User admin = userService.findByEmail(adminEmail);
            if (admin == null || admin.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Admin can trigger Auto-Assign");
            }
            complaintService.autoAssignStaff(complaintId);
            return ResponseEntity.ok("Staff Auto-Assigned Successfully using AI/Workload Logic! 🤖");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}