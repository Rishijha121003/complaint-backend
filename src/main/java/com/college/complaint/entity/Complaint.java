package com.college.complaint.entity;

import com.college.complaint.enums.ComplaintCategory;
import com.college.complaint.enums.ComplaintStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Student who raised complaint
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Staff assigned by admin
    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    // Automatically set before insert
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = ComplaintStatus.OPEN;
    }

    // Automatically update time before update
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public User getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(User assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    // Complaint.java ke andar fields ke saath ye add karein
    private String location;

    private String imageUrl; // For file attachment

    // Aur ye methods (Getters/Setters) add karein
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private String resolvedImageUrl; // For Staff Proof
    private String staffRemark; // Staff's final comment

    public String getResolvedImageUrl() {
        return resolvedImageUrl;
    }

    public void setResolvedImageUrl(String resolvedImageUrl) {
        this.resolvedImageUrl = resolvedImageUrl;
    }

    public String getStaffRemark() {
        return staffRemark;
    }

    public void setStaffRemark(String staffRemark) {
        this.staffRemark = staffRemark;
    }
}
