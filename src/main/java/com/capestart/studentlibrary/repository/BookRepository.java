package com.capestart.studentlibrary.repository;

import com.capestart.studentlibrary.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByStudentIsNull();

//    List<Book> findByStudentId(Long studentId);
//
//    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE b.student.id = :studentId AND b.id = :bookId")
    Optional<Book> findByStudentIdAndBookId(Long studentId, Long bookId);
}