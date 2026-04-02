package com.capestart.studentlibrary.repository;

import com.capestart.studentlibrary.entity.Book;
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
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b WHERE b.student IS NULL ORDER BY b.id ASC")
    List<Book> findByStudentIsNull();

    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE b.student.id = :studentId AND b.id = :bookId")
    Optional<Book> findByStudentIdAndBookId(Long studentId, Long bookId);

    @Query("SELECT b FROM Book b ORDER BY b.id ASC")
    List<Book> findAllOrderById();

    List<Book> findByStudentIdOrderByIdAsc(Long studentId);

    @Query("SELECT b.isbn FROM Book b WHERE b.isbn IN :isbns")
    Set<String> findExistingIsbns(@Param("isbns") Set<String> isbns);

    // ADD THIS TO BookRepository.java
    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Book> searchBooks(@Param("search") String search, Pageable pageable);
}
