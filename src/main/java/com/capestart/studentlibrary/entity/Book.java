package com.capestart.studentlibrary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "genre", nullable = false, length = 100)
    private String genre;

    @Column(name = "isbn", nullable = false, unique = true, length = 50)
    private String isbn;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
}