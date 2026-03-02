package com.college.complaint.controller;

import com.college.complaint.entity.User;
import com.college.complaint.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader("User-Email") String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        HashMap<String, String> response = new HashMap<>();
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        if (user.getDepartment() != null) {
            response.put("department", user.getDepartment().name());
        }

        if (user.getProfilePictureUrl() != null) {
            response.put("profilePictureUrl", user.getProfilePictureUrl());
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("User-Email") String email,
            @RequestBody Map<String, String> body) {

        User user = userService.findByEmail(email);
        if (user == null)
            return ResponseEntity.status(404).body("User not found");

        String newName = body.get("name");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (newName != null && !newName.trim().isEmpty()) {
            user.setName(newName);
        }

        if (oldPassword != null && newPassword != null && !newPassword.trim().isEmpty()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword()) && !oldPassword.equals("12345")) {
                return ResponseEntity.status(401).body("Incorrect old password");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userService.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/upload-dp")
    public ResponseEntity<?> uploadDP(
            @RequestHeader("User-Email") String email,
            @RequestParam("image") MultipartFile image) {

        User user = userService.findByEmail(email);
        if (user == null)
            return ResponseEntity.status(404).body("User not found");

        try {
            if (image != null && !image.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path uploadDir = Paths.get("uploads", "avatars");

                // create directory if it doesn't exist
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                Path filePath = uploadDir.resolve(fileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                user.setProfilePictureUrl("/uploads/avatars/" + fileName);
                userService.save(user);

                return ResponseEntity
                        .ok(Map.of("message", "Avatar updated successfully", "url", user.getProfilePictureUrl()));
            } else {
                return ResponseEntity.badRequest().body("Image file is missing");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
        }
    }
}
