package com.college.complaint.config;

import com.college.complaint.entity.Category;
import com.college.complaint.entity.Domain;
import com.college.complaint.repository.CategoryRepository;
import com.college.complaint.repository.DomainRepository;
import com.college.complaint.repository.UserRepository;
import com.college.complaint.repository.ComplaintRepository;
import com.college.complaint.entity.User;
import com.college.complaint.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

        @Bean
        CommandLineRunner initDatabase(DomainRepository domainRepository, 
                                       CategoryRepository categoryRepository,
                                       UserRepository userRepository,
                                       ComplaintRepository complaintRepository,
                                       PasswordEncoder passwordEncoder) {
                return args -> {
                        // 1. Structural Data (Domains/Categories)
                        if (domainRepository.count() == 0) {
                                Domain maintenance = domainRepository.save(new Domain("Maintenance", "Infrastructure, electrical, plumbing, etc."));
                                categoryRepository.save(new Category("Electrical Issue", maintenance));
                                categoryRepository.save(new Category("Plumbing Issue", maintenance));
                                
                                Domain itServices = domainRepository.save(new Domain("IT Services", "Connectivity and portal issues"));
                                categoryRepository.save(new Category("WiFi Issue", itServices));
                        }

                        // 2. Identity & Activity Data (Users/Complaints)
                        if (userRepository.count() == 0) {
                                Domain maintenance = domainRepository.findAll().get(0);
                                Category elec = categoryRepository.findAll().stream()
                                        .filter(c -> c.getDomain().getName().equals("Maintenance"))
                                        .findFirst().orElse(null);

                                // Master Admin
                                User admin = new User();
                                admin.setName("Super Admin");
                                admin.setEmail("admin@test.com");
                                admin.setPassword(passwordEncoder.encode("admin123"));
                                admin.setRole(Role.ADMIN);
                                userRepository.save(admin);

                                // Specialization Test Staff
                                User staff = new User();
                                staff.setName("Deepak Technician");
                                staff.setEmail("deepak@test.com");
                                staff.setPassword(passwordEncoder.encode("staff123"));
                                staff.setRole(Role.STAFF);
                                staff.setDepartment(maintenance);
                                staff.setSpecialization(elec);
                                userRepository.save(staff);

                                User student = new User();
                                student.setName("Rishi Student");
                                student.setEmail("student@test.com");
                                student.setPassword(passwordEncoder.encode("student123"));
                                student.setRole(Role.STUDENT);
                                userRepository.save(student);

                                // Test Activity
                                com.college.complaint.entity.Complaint c1 = new com.college.complaint.entity.Complaint();
                                c1.setTitle("Critical: Fan issue in Library");
                                c1.setDescription("Fan is spinning slowly and smoking.");
                                c1.setStatus(com.college.complaint.enums.ComplaintStatus.OPEN);
                                c1.setPriority(com.college.complaint.enums.ComplaintPriority.HIGH);
                                c1.setStudent(student);
                                c1.setCategory(elec);
                                complaintRepository.save(c1);
                        }
                };
        }
}
