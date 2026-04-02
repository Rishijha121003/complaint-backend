package com.college.complaint.controller;

import com.college.complaint.entity.Category;
import com.college.complaint.entity.Domain;
import com.college.complaint.repository.CategoryRepository;
import com.college.complaint.repository.DomainRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class CategoryController {

    private final DomainRepository domainRepository;
    private final CategoryRepository categoryRepository;

    public CategoryController(DomainRepository domainRepository, CategoryRepository categoryRepository) {
        this.domainRepository = domainRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/domains")
    public List<Domain> getAllDomains() {
        return domainRepository.findAll();
    }

    @GetMapping("/categories/{domainId}")
    public List<Category> getCategoriesByDomain(@PathVariable Long domainId) {
        Domain domain = domainRepository.findById(domainId).orElseThrow(() -> new RuntimeException("Domain not found"));
        return categoryRepository.findByDomain(domain);
    }
}
