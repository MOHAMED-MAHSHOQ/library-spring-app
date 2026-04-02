package com.capestart.studentlibrary.controller;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.capestart.studentlibrary.service.BookImportService;
import com.capestart.studentlibrary.service.StudentImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
public class CsvImportController {
    private final StudentImportService studentImportService;
    private final BookImportService bookImportService;

    @PostMapping("/students")
    public ResponseEntity<ImportResultDto> importStudents(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().build();
        }

        ImportResultDto result = studentImportService.importStudents(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/books")
    public ResponseEntity<ImportResultDto> importBooks(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().build();
        }

        ImportResultDto result = bookImportService.importBooks(file);
        return ResponseEntity.ok(result);
    }
}