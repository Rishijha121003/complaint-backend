package com.college.complaint.service;

import com.college.complaint.entity.User;
import com.college.complaint.enums.Role;
import com.college.complaint.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    /**
     * SECURED USER CREATION (Admin Managed)
     * Validates input according to production standards.
     */
    public User createAdminManagedUser(User newUser) {
        // 1. Validation: Name must not be empty
        if (newUser.getName() == null || newUser.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full Legal Name is required.");
        }

        // 2. Validation: Email uniqueness & format
        if (newUser.getEmail() == null || !newUser.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new IllegalArgumentException("Identity already exists with this email.");
        }

        // 3. Validation: Password length
        if (newUser.getPassword() == null || newUser.getPassword().length() < 6) {
            throw new IllegalArgumentException("Security integrity violation: Password must be at least 6 characters.");
        }

        // 4. Authorization Check: Prevent creation of ADMINS from this endpoint
        if (newUser.getRole() == Role.ADMIN) {
            throw new SecurityException(
                    "Authority violation: Only STAFF and STUDENT identities can be spawned via this channel.");
        }

        // 5. Security: Always encode password
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        return userRepository.save(newUser);
    }
}