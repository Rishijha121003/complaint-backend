package com.college.complaint.repository;

import com.college.complaint.entity.User;
import com.college.complaint.enums.Role;
import com.college.complaint.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    // Ye line add karna zaroori hai
    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndDepartment(Role role, Domain department);
}