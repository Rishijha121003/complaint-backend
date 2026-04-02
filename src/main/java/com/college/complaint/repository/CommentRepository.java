package com.college.complaint.repository;

import com.college.complaint.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
