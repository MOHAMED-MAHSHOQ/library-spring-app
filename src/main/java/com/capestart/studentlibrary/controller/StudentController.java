package com.capestart.studentlibrary.controller;

import com.capestart.studentlibrary.dto.request.StudentRequestDto;
import com.capestart.studentlibrary.dto.response.StudentResponseDto;
import com.capestart.studentlibrary.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponseDto> createStudent(
            @Valid @RequestBody StudentRequestDto requestDto) {
        StudentResponseDto response = studentService.createStudent(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<?> getAllStudents(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "id")   String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {

        // if no search, return paged
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(studentService.getAllStudentsPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        StudentResponseDto response = studentService.getStudentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto requestDto) {
        StudentResponseDto response = studentService.updateStudent(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<StudentResponseDto> assignBookToStudent(
            @PathVariable Long studentId,
            @PathVariable Long bookId) {
        StudentResponseDto response = studentService.assignBookToStudent(studentId, bookId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<StudentResponseDto> removeBookFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long bookId) {
        StudentResponseDto response = studentService.removeBookFromStudent(studentId, bookId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchStudents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        return ResponseEntity.ok(studentService.searchStudents(query, page, size));
    }
}