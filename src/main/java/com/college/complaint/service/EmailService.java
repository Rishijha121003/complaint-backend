package com.college.complaint.service;

import com.college.complaint.entity.Comment;
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmailId;

    @Value("${app.name:CampusDesk}")
    private String appName;

    @Value("${app.url:https://complaint-backend-5rdk.onrender.com}")
    private String appUrl;

    /**
     * Minimal, Async Email Sender with structured logging and fail-safe handling.
     */
    @Async
    public void sendEmail(String to, String subject, String body, Long ticketId, String type) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailId != null ? fromEmailId : "noreply@campusdesk.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            logger.info("Email sent | TicketId={} | To={} | Type={}", ticketId, to, type);
        } catch (Exception e) {
            logger.error("Email failed | TicketId={} | To={}", ticketId, to);
            // Non-blocking fail-safe: logic ends here
        }
    }

    // --- CLEAN NOTIFICATION TEMPLATES (NO EMOJIS) ---

    public void notifyStudentTicketRaised(Complaint complaint) {
        String subject = "Complaint Update - Ticket #" + complaint.getId();
        String body = String.format(
            "Dear %s,\n\n" +
            "Your complaint has been successfully registered on %s.\n\n" +
            "Ticket ID: #%d\n" +
            "Title: %s\n" +
            "Status: %s\n\n" +
            "Track your ticket here: %s\n\n" +
            "Regards,\n" +
            "Team %s",
            complaint.getStudent().getName(), appName, complaint.getId(),
            complaint.getTitle(), complaint.getStatus(), appUrl, appName
        );
        sendEmail(complaint.getStudent().getEmail(), subject, body, complaint.getId(), "TICKET_RAISED");
    }

    public void notifyStaffAssignment(Complaint complaint) {
        if (complaint.getAssignedStaff() == null) return;
        String subject = "Complaint Update - Ticket #" + complaint.getId();
        String body = String.format(
            "Dear %s,\n\n" +
            "A new ticket has been assigned to you for resolution.\n\n" +
            "Ticket ID: #%d\n" +
            "Title: %s\n" +
            "Status: %s\n\n" +
            "View ticket on dashboard: %s\n\n" +
            "Regards,\n" +
            "Team %s",
            complaint.getAssignedStaff().getName(), complaint.getId(),
            complaint.getTitle(), complaint.getStatus(), appUrl, appName
        );
        sendEmail(complaint.getAssignedStaff().getEmail(), subject, body, complaint.getId(), "STAFF_ASSIGNED");
    }

    public void notifyStatusUpdate(Complaint complaint, String remark) {
        String subject = "Complaint Update - Ticket #" + complaint.getId();
        String body = String.format(
            "Dear %s,\n\n" +
            "The status of your ticket #%d has been updated to %s.\n\n" +
            "Remark: %s\n\n" +
            "Check details here: %s\n\n" +
            "Regards,\n" +
            "Team %s",
            complaint.getStudent().getName(), complaint.getId(),
            complaint.getStatus(), (remark != null ? remark : "None"), appUrl, appName
        );
        sendEmail(complaint.getStudent().getEmail(), subject, body, complaint.getId(), "STATUS_UPDATE");
    }

    public void notifyNewComment(Complaint complaint, Comment comment, User recipient) {
        String subject = "Complaint Update - Ticket #" + complaint.getId();
        String body = String.format(
            "Dear %s,\n\n" +
            "You have received a new message regarding Ticket #%d.\n\n" +
            "Message Content:\n" +
            "\"%s\"\n\n" +
            "Reply through the dashboard: %s\n\n" +
            "Regards,\n" +
            "Team %s",
            recipient.getName(), complaint.getId(), comment.getText(), appUrl, appName
        );
        sendEmail(recipient.getEmail(), subject, body, complaint.getId(), "NEW_COMMENT");
    }

    public void notifyTicketClosed(Complaint complaint) {
        if (complaint.getAssignedStaff() == null) return;
        String subject = "Complaint Update - Ticket #" + complaint.getId();
        String body = String.format(
            "Dear %s,\n\n" +
            "The ticket #%d has been confirmed and closed by the student.\n\n" +
            "Regards,\n" +
            "Team %s",
            complaint.getAssignedStaff().getName(), complaint.getId(), appName
        );
        sendEmail(complaint.getAssignedStaff().getEmail(), subject, body, complaint.getId(), "TICKET_CLOSED");
    }
}
