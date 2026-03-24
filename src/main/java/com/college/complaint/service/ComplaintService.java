package com.college.complaint.service;

import com.college.complaint.dto.ComplaintRequest;
import com.college.complaint.entity.Category;
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.enums.ComplaintPriority;
import com.college.complaint.enums.ComplaintStatus;
import com.college.complaint.enums.Role;
import com.college.complaint.repository.CategoryRepository;
import com.college.complaint.repository.ComplaintRepository;
import com.college.complaint.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EmailService emailService;

    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository,
            CategoryRepository categoryRepository, EmailService emailService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.emailService = emailService;
    }

    public Complaint saveComplaint(ComplaintRequest request, String email) {
        User student = userRepository.findByEmail(email);

        if (student == null) {
            throw new RuntimeException("Error: User with email " + email + " not found!");
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());

        // Category matching - Try ID first, then fallback to name
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        if (category == null) {
            // Optional fallback if category string matches
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                List<Category> allCategories = categoryRepository.findAll();
                category = allCategories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(request.getCategory()))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (category != null) {
            complaint.setCategory(category);
        }

        try {
            String priorityStr = request.getPriority();
            if (priorityStr != null && !priorityStr.isEmpty()) {
                complaint.setPriority(ComplaintPriority.valueOf(priorityStr.toUpperCase()));
            } else {
                complaint.setPriority(ComplaintPriority.LOW);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            complaint.setPriority(ComplaintPriority.LOW);
        }

        complaint.setLocation(request.getLocation());
        if (request.getImageUrl() != null) {
            complaint.setImageUrl(request.getImageUrl());
        }
        complaint.setStudent(student);
        complaint.setStatus(ComplaintStatus.OPEN);

        Complaint savedComplaint = complaintRepository.save(complaint);

        emailService.sendEmail(
                student.getEmail(),
                "Complaint Registered Successfully: " + savedComplaint.getTitle(),
                "Dear " + student.getName() + ",\n\nYour complaint has been successfully registered with ID: #"
                        + savedComplaint.getId() + "\nCategory: " + (savedComplaint.getCategory() != null ? savedComplaint.getCategory().getName() : "Unassigned")
                        + "\nStatus: OPEN\n\nWe will review this shortly.\n\nSmart Complaint System");

        return savedComplaint;
    }

    public Complaint assignStaff(Long complaintId, User admin, User staff, Long categoryId) {
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can assign staff");
        }
        if (staff.getRole() != Role.STAFF) {
            throw new RuntimeException("Assigned user must be staff");
        }

        Complaint complaint = getComplaintOrThrow(complaintId);
        if (complaint.getStatus() != ComplaintStatus.OPEN && complaint.getStatus() != ComplaintStatus.ASSIGNED && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new RuntimeException("Only OPEN, ASSIGNED, or IN_PROGRESS complaints can be assigned");
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                complaint.setCategory(category);
            }
        }

        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        emailService.sendEmail(
                staff.getEmail(),
                "New Complaint Assigned: #" + updatedComplaint.getId(),
                "Dear " + staff.getName() + ",\n\nA new complaint has been assigned to you.\n\nTitle: "
                        + updatedComplaint.getTitle() + "\nCategory: " + (updatedComplaint.getCategory() != null ? updatedComplaint.getCategory().getName() : "Unassigned")
                        + "\n\nPlease check your dashboard for details.\n\nSmart Complaint System");

        return updatedComplaint;
    }

    public Complaint updateStatus(Long complaintId, ComplaintStatus newStatus, User currentUser, String staffRemark,
            String resolvedImageUrl) {
        Complaint complaint = getComplaintOrThrow(complaintId);
        validateRolePermission(complaint, newStatus, currentUser);
        validateStatusTransition(complaint.getStatus(), newStatus);

        if (staffRemark != null) {
            complaint.setStaffRemark(staffRemark);
        }
        if (resolvedImageUrl != null) {
            complaint.setResolvedImageUrl(resolvedImageUrl);
        }

        complaint.setStatus(newStatus);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        if (newStatus == ComplaintStatus.RESOLVED || newStatus == ComplaintStatus.REJECTED) {
            emailService.sendEmail(
                    updatedComplaint.getStudent().getEmail(),
                    "Complaint Status Changed: #" + updatedComplaint.getId(),
                    "Dear " + updatedComplaint.getStudent().getName() + ",\n\nThe status of your complaint (ID: #"
                            + updatedComplaint.getId() + ") has been updated to: " + newStatus + ".\n" +
                            (staffRemark != null ? "Remark from Staff: " + staffRemark + "\n\n" : "\n") +
                            "Log in to your dashboard to view more details.\n\nSmart Complaint System");
        }

        return updatedComplaint;
    }

    public Complaint closeComplaint(Long complaintId, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("Only student can close complaint");
        }

        Complaint complaint = getComplaintOrThrow(complaintId);
        if (!complaint.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("You can only close your own complaint");
        }
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new RuntimeException("Only resolved complaints can be closed");
        }

        complaint.setStatus(ComplaintStatus.CLOSED);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        if (updatedComplaint.getAssignedStaff() != null) {
            emailService.sendEmail(
                    updatedComplaint.getAssignedStaff().getEmail(),
                    "Complaint Closed: #" + updatedComplaint.getId(),
                    "Dear " + updatedComplaint.getAssignedStaff().getName() + ",\n\nThe complaint (ID: #"
                            + updatedComplaint.getId()
                            + ") has been confirmed and CLOSED by the student.\n\nThank you for your assistance.\n\nSmart Complaint System");
        }

        return updatedComplaint;
    }

    private void validateStatusTransition(ComplaintStatus current, ComplaintStatus next) {
        switch (current) {
            case OPEN:
                if (next != ComplaintStatus.ASSIGNED && next != ComplaintStatus.REJECTED) {
                    throw new RuntimeException("Invalid transition from OPEN");
                }
                break;
            case ASSIGNED:
                if (next != ComplaintStatus.IN_PROGRESS) {
                    throw new RuntimeException("Invalid transition from ASSIGNED");
                }
                break;
            case IN_PROGRESS:
                if (next != ComplaintStatus.RESOLVED) {
                    throw new RuntimeException("Invalid transition from IN_PROGRESS");
                }
                break;
            case RESOLVED:
                if (next != ComplaintStatus.CLOSED) {
                    throw new RuntimeException("Invalid transition from RESOLVED");
                }
                break;
            default:
                throw new RuntimeException("Invalid status transition");
        }
    }

    private void validateRolePermission(Complaint complaint, ComplaintStatus newStatus, User user) {
        Role role = user.getRole();
        switch (role) {
            case ADMIN:
                if (newStatus != ComplaintStatus.ASSIGNED && newStatus != ComplaintStatus.REJECTED) {
                    throw new RuntimeException("Admin can only assign or reject");
                }
                break;
            case STAFF:
                if (complaint.getAssignedStaff() == null
                        || !complaint.getAssignedStaff().getId().equals(user.getId())) {
                    throw new RuntimeException("You can only update assigned complaints");
                }
                if (newStatus != ComplaintStatus.IN_PROGRESS && newStatus != ComplaintStatus.RESOLVED) {
                    throw new RuntimeException("Staff can only move to IN_PROGRESS or RESOLVED");
                }
                break;
            case STUDENT:
                throw new RuntimeException("Student cannot update status directly");
            default:
                throw new RuntimeException("Unauthorized action");
        }
    }

    private Complaint getComplaintOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    public List<Complaint> getComplaintsByStudent(User student) {
        return complaintRepository.findByStudent(student);
    }

    public List<Complaint> getComplaintsByStaff(User staff) {
        return complaintRepository.findByAssignedStaff(staff);
    }

    public List<Complaint> getOpenComplaints() {
        return complaintRepository.findByStatus(ComplaintStatus.OPEN);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

}