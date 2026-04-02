package com.capestart.studentlibrary.repository;

import com.capestart.studentlibrary.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT s FROM Student s ORDER BY s.id ASC")
    List<Student> findAllOrderById();

    @Query("SELECT s.email FROM Student s WHERE s.email IN :emails")
    Set<String> findExistingEmails(@Param("emails") Set<String> emails);

    @Query("SELECT s FROM Student s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.department) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Student> searchByNameOrDepartmentOrEmail(
            @Param("query") String query, Pageable pageable);
}