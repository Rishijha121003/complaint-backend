package com.college.complaint.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminComplaintDTO {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private String studentName;
    private String assignedStaffName;
    private String categoryName;
    private String location;
}
