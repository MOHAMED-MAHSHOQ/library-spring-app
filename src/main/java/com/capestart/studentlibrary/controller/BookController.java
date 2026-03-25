package com.capestart.studentlibrary.controller;

import com.capestart.studentlibrary.dto.request.BookRequestDto;
import com.capestart.studentlibrary.dto.response.BookResponseDto;
import com.capestart.studentlibrary.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<BookResponseDto> createBook(
            @Valid @RequestBody BookRequestDto requestDto) {
        BookResponseDto response = bookService.createBook(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @GetMapping
//    public ResponseEntity<List<BookResponseDto>> getAllBooks() {
//        List<BookResponseDto> response = bookService.getAllBooks();
//        return ResponseEntity.ok(response);
//    }

    @GetMapping
    public ResponseEntity<?> getAllBooks(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(bookService.getAllBooksPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        BookResponseDto response = bookService.getBookById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDto> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequestDto requestDto) {
        BookResponseDto response = bookService.updateBook(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<BookResponseDto>> getUnassignedBooks() {
        List<BookResponseDto> response = bookService.getUnassignedBooks();
        return ResponseEntity.ok(response);
    }
}