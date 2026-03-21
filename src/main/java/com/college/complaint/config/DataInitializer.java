package com.college.complaint.config;

import com.college.complaint.entity.Category;
import com.college.complaint.entity.Domain;
import com.college.complaint.repository.CategoryRepository;
import com.college.complaint.repository.DomainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(DomainRepository domainRepository, CategoryRepository categoryRepository) {
        return args -> {
            if (domainRepository.count() == 0) {
                // 1. Maintenance Domain
                Domain maintenance = domainRepository
                        .save(new Domain("Maintenance", "Infrastructure, electrical, plumbing, etc."));
                categoryRepository.save(new Category("Electrical Issue", maintenance));
                categoryRepository.save(new Category("Plumbing Issue", maintenance));
                categoryRepository.save(new Category("Elevator Issue", maintenance));
                categoryRepository.save(new Category("Infrastructure Damage", maintenance));
                categoryRepository.save(new Category("Cleanliness Issue", maintenance));

                // 2. IT Services Domain
                Domain itServices = domainRepository
                        .save(new Domain("IT Services", "Internet, WiFi, Portal, and Login issues"));
                categoryRepository.save(new Category("Internet Connectivity Issue", itServices));
                categoryRepository.save(new Category("WiFi Not Working", itServices));
                categoryRepository.save(new Category("Portal Issue", itServices));
                categoryRepository.save(new Category("Login Issue", itServices));

                // 3. Academics Domain
                Domain academics = domainRepository
                        .save(new Domain("Academics", "Exams, marks, attendance, and timetable"));
                categoryRepository.save(new Category("Marks / Result Issue", academics));
                categoryRepository.save(new Category("Attendance Issue", academics));
                categoryRepository.save(new Category("Exam Issue", academics));
                categoryRepository.save(new Category("Timetable Issue", academics));

                // 4. Hostel Domain
                Domain hostel = domainRepository
                        .save(new Domain("Hostel", "Rooms, mess, cleaning, and facilities"));
                categoryRepository.save(new Category("Room Maintenance Issue", hostel));
                categoryRepository.save(new Category("Mess Food Issue", hostel));
                categoryRepository.save(new Category("Bathroom / Water Issue", hostel));
                categoryRepository.save(new Category("Cleaning Issue", hostel));

                // 5. Administration Domain
                Domain admin = domainRepository
                        .save(new Domain("Administration", "Fees, scholarships, documents, etc."));
                categoryRepository.save(new Category("Fee Payment Issue", admin));
                categoryRepository.save(new Category("Scholarship Issue", admin));
                categoryRepository.save(new Category("Document / Certificate Issue", admin));
                categoryRepository.save(new Category("ID Card Issue", admin));
            }
        };
    }
}
