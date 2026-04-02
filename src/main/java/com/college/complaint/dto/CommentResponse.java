package com.college.complaint.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    private Long id;
    private String text;
    private String senderName;
    private String senderRole;
    private String senderProfilePic;
    private LocalDateTime createdAt;

    public CommentResponse(Long id, String text, String senderName, String senderRole, String senderProfilePic,
            LocalDateTime createdAt) {
        this.id = id;
        this.text = text;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.senderProfilePic = senderProfilePic;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getSenderProfilePic() {
        return senderProfilePic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
