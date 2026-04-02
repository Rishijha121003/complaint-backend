package com.college.complaint.repository;

import com.college.complaint.entity.Category;
import com.college.complaint.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDomain(Domain domain);

    Category findByNameAndDomain(String name, Domain domain);
}
