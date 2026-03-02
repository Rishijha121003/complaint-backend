package com.college.complaint.repository;

import com.college.complaint.entity.Complaint;
import com.college.complaint.entity.User;
import com.college.complaint.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStudent(User student);

    List<Complaint> findByAssignedStaff(User staff);

    List<Complaint> findByStatus(ComplaintStatus status);
}
