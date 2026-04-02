package com.college.complaint.controller;

import com.college.complaint.dto.CommentRequest;
import com.college.complaint.dto.CommentResponse;
import com.college.complaint.entity.Comment;
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.repository.CommentRepository;
import com.college.complaint.repository.ComplaintRepository;
import com.college.complaint.service.UserService;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentRepository commentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserService userService;
    private final com.college.complaint.service.EmailService emailService;

    public CommentController(CommentRepository commentRepository, ComplaintRepository complaintRepository,
            UserService userService, com.college.complaint.service.EmailService emailService) {
        this.commentRepository = commentRepository;
        this.complaintRepository = complaintRepository;
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<?> getComments(@PathVariable Long complaintId) {
        List<Comment> comments = commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        List<CommentResponse> response = comments.stream().map(c -> new CommentResponse(
                c.getId(),
                c.getText(),
                c.getSender().getName(),
                c.getSender().getRole().name(),
                c.getSender().getProfilePictureUrl(),
                c.getCreatedAt())).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{complaintId}")
    public ResponseEntity<?> addComment(
            @PathVariable Long complaintId,
            @RequestBody CommentRequest request,
            @RequestHeader(value = "User-Email", required = false) String email) {

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(401).body("User-Email header is missing");
        }

        User sender = userService.findByEmail(email);
        if (sender == null)
            return ResponseEntity.status(404).body("User not found");

        Long cId = complaintId;
        Complaint complaint = (cId != null) ? complaintRepository.findById(cId).orElse(null) : null;
        if (complaint == null)
            return ResponseEntity.status(404).body("Complaint not found");

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setSender(sender);
        comment.setComplaint(complaint);

        Comment saved = commentRepository.save(comment);

        // --- EMAIL NOTIFICATION LOGIC ---
        try {
            if (sender.getRole() == com.college.complaint.enums.Role.STUDENT) {
                // If student comments, notify the assigned staff
                if (complaint.getAssignedStaff() != null) {
                    emailService.notifyNewComment(complaint, saved, complaint.getAssignedStaff());
                }
            } else {
                // If staff/admin comments, notify the student
                emailService.notifyNewComment(complaint, saved, complaint.getStudent());
            }
        } catch (Exception e) {
            logger.error("Failed to send comment notification email: {}", e.getMessage());
        }

        return ResponseEntity.ok(new CommentResponse(
                saved.getId(),
                saved.getText(),
                saved.getSender().getName(),
                saved.getSender().getRole().name(),
                saved.getSender().getProfilePictureUrl(),
                saved.getCreatedAt()));
    }
}
