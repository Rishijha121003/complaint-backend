package com.college.complaint.service;

import com.college.complaint.dto.ComplaintRequest;
import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.enums.ComplaintCategory; // Add this
import com.college.complaint.enums.ComplaintStatus;
import com.college.complaint.enums.Role;
import com.college.complaint.repository.ComplaintRepository;
import com.college.complaint.repository.UserRepository; // Add this
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService {

    // 1. Pehle declare karein
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository; // Ye line check kar
    private final EmailService emailService;

    // 2. Constructor mein dono ko pass karein
    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository,
            EmailService emailService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository; // Ye injection zaroori hai
        this.emailService = emailService;
    }

    // ====================== =======
    // 1️⃣ Student creates complaint
    // =============================
    public Complaint saveComplaint(ComplaintRequest request, String email) {
        User student = userRepository.findByEmail(email);

        // 1. User check (Principal is null error se bachne ke liye)
        if (student == null) {
            throw new RuntimeException("Error: User with email " + email + " not found!");
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());

        // 2. Enum conversion with Try-Catch (Red line aur 500 error ka permanent ilaj)
        try {
            String categoryStr = request.getCategory();
            if (categoryStr != null && !categoryStr.isEmpty()) {
                complaint.setCategory(ComplaintCategory.valueOf(categoryStr.toUpperCase()));
            } else {
                complaint.setCategory(ComplaintCategory.OTHER);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // Agar Enum match nahi hua (jaise 'OTHER' missing ho), toh ye default set
            // karega
            complaint.setCategory(ComplaintCategory.OTHER);
        }

        complaint.setLocation(request.getLocation());
        if (request.getImageUrl() != null) {
            complaint.setImageUrl(request.getImageUrl());
        }
        complaint.setStudent(student);
        complaint.setStatus(ComplaintStatus.OPEN);

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Send Email Notification
        emailService.sendEmail(
                student.getEmail(),
                "Complaint Registered Successfully: " + savedComplaint.getTitle(),
                "Dear " + student.getName() + ",\n\nYour complaint has been successfully registered with ID: #"
                        + savedComplaint.getId() + "\nCategory: " + savedComplaint.getCategory()
                        + "\nStatus: OPEN\n\nWe will review this shortly.\n\nSmart Complaint System");

        return savedComplaint;
    }

    // =============================
    // 2️⃣ Admin assigns staff
    // =============================
    public Complaint assignStaff(Long complaintId, User admin, User staff) {
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can assign staff");
        }
        if (staff.getRole() != Role.STAFF) {
            throw new RuntimeException("Assigned user must be staff");
        }

        Complaint complaint = getComplaintOrThrow(complaintId);
        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new RuntimeException("Only OPEN complaints can be assigned");
        }

        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        // Notify Staff
        emailService.sendEmail(
                staff.getEmail(),
                "New Complaint Assigned: #" + updatedComplaint.getId(),
                "Dear " + staff.getName() + ",\n\nA new complaint has been assigned to you.\n\nTitle: "
                        + updatedComplaint.getTitle() + "\nCategory: " + updatedComplaint.getCategory()
                        + "\n\nPlease check your dashboard for details.\n\nSmart Complaint System");

        // Notify Student
        emailService.sendEmail(
                updatedComplaint.getStudent().getEmail(),
                "Complaint Update: #" + updatedComplaint.getId(),
                "Dear " + updatedComplaint.getStudent().getName() + ",\n\nYour complaint (ID: #"
                        + updatedComplaint.getId() + ") has been ASSIGNED to staff member " + staff.getName()
                        + " for resolution.\n\nSmart Complaint System");

        return updatedComplaint;
    }

    // =============================
    // 3️⃣ Staff / Admin updates status
    // =============================
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

        // Notify Student about status change
        emailService.sendEmail(
                updatedComplaint.getStudent().getEmail(),
                "Complaint Status Changed: #" + updatedComplaint.getId(),
                "Dear " + updatedComplaint.getStudent().getName() + ",\n\nThe status of your complaint (ID: #"
                        + updatedComplaint.getId() + ") has been updated to: " + newStatus + ".\n" +
                        (staffRemark != null ? "Remark from Staff: " + staffRemark + "\n\n" : "\n") +
                        "Log in to your dashboard to view more details.\n\nSmart Complaint System");

        return updatedComplaint;
    }

    // =============================
    // 4️⃣ Student closes complaint
    // =============================
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

        // Notify Staff that Student closed the complaint
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

    // =============================
    // 🔥 STATUS FLOW VALIDATION
    // =============================
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

    // =============================
    // 🤖 AI/Auto-Assign Staff
    // =============================
    public Complaint autoAssignStaff(Long complaintId) {
        Complaint complaint = getComplaintOrThrow(complaintId);
        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new RuntimeException("Only OPEN complaints can be automatically assigned");
        }

        // Us category ke saare staff dhundho
        List<User> matchingStaff = userRepository.findByRoleAndDepartment(Role.STAFF, complaint.getCategory());

        if (matchingStaff == null || matchingStaff.isEmpty()) {
            // Fallback: Agar specific category ka staff na mile, toh system ka koi bhi
            // staff pick karo
            matchingStaff = userRepository.findByRole(Role.STAFF);
            if (matchingStaff == null || matchingStaff.isEmpty()) {
                throw new RuntimeException("No staff available in the system for assignment");
            }
        }

        // Logic: Sabse kam assigned items wala staff chuno
        User bestStaff = null;
        int minWorkload = Integer.MAX_VALUE;

        for (User staff : matchingStaff) {
            // "IN_PROGRESS" status wali complaints ginenge jo is staff ko assigned hain
            int currentWorkload = (int) complaintRepository.findByAssignedStaff(staff).stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS
                            || c.getStatus() == ComplaintStatus.ASSIGNED)
                    .count();

            if (currentWorkload < minWorkload) {
                minWorkload = currentWorkload;
                bestStaff = staff;
            }
        }

        if (bestStaff == null) {
            bestStaff = matchingStaff.get(0); // Default to first available
        }

        complaint.setAssignedStaff(bestStaff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        // System (Auto-Assigner) notification email (optional - skipping full detail
        // here for brevity)
        emailService.sendEmail(
                bestStaff.getEmail(),
                "New Complaint AUTO-ASSIGNED to You: #" + updatedComplaint.getId(),
                "Dear " + bestStaff.getName()
                        + ",\n\nA new complaint has been auto-assigned to you due to your current workload optimization.\n\nTitle: "
                        + updatedComplaint.getTitle() + "\nCategory: " + updatedComplaint.getCategory()
                        + "\n\nPlease check your dashboard for details.\n\nSmart Complaint System");

        return updatedComplaint;
    }
}