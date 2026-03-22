package com.capestart.studentlibrary.repository;

import com.capestart.studentlibrary.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT s FROM Student s ORDER BY s.id ASC")
    List<Student> findAllOrderById();
}