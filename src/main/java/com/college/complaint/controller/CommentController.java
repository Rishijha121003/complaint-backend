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
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserService userService;

    public CommentController(CommentRepository commentRepository, ComplaintRepository complaintRepository,
            UserService userService) {
        this.commentRepository = commentRepository;
        this.complaintRepository = complaintRepository;
        this.userService = userService;
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

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null)
            return ResponseEntity.status(404).body("Complaint not found");

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setSender(sender);
        comment.setComplaint(complaint);

        Comment saved = commentRepository.save(comment);

        return ResponseEntity.ok(new CommentResponse(
                saved.getId(),
                saved.getText(),
                saved.getSender().getName(),
                saved.getSender().getRole().name(),
                saved.getSender().getProfilePictureUrl(),
                saved.getCreatedAt()));
    }
}
