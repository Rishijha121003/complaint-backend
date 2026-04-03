package com.college.complaint.dto;

import com.college.complaint.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String departmentName;
    private String specializationName;
    private String profilePictureUrl;
}
