package com.capestart.studentlibrary.service.impl;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.capestart.studentlibrary.entity.Book;
import com.capestart.studentlibrary.entity.Student;
import com.capestart.studentlibrary.repository.BookRepository;
import com.capestart.studentlibrary.repository.StudentRepository;
import com.capestart.studentlibrary.service.CsvImportService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportServiceImpl implements CsvImportService {

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;

    // How many records to save at once — prevents memory overload
    private static final int BATCH_SIZE = 500;

    @Override
    public ImportResultDto importStudents(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0, imported = 0, skipped = 0, failed = 0;
        List<Student> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)))) {

            String[] headers = reader.readNext();
            // skip the header row — first line is column names

            if (headers == null) {
                errors.add("CSV file is empty");
                return buildResult(0, 0, 0, 1, errors);
            }

            String[] row;
            int rowNum = 2;
            // start at 2 because row 1 was the header

            while ((row = reader.readNext()) != null) {
                totalRows++;

                try {
                    // CSV format: name, email, phone, department
                    if (row.length < 4) {
                        errors.add("Row " + rowNum + ": Not enough columns (expected 4)");
                        failed++;
                        rowNum++;
                        continue;
                    }

                    String name       = row[0].trim();
                    String email      = row[1].trim();
                    String phone      = row[2].trim();
                    String department = row[3].trim();

                    // Basic validation
                    if (name.isEmpty() || email.isEmpty() || department.isEmpty()) {
                        errors.add("Row " + rowNum + ": Name, email, department are required");
                        failed++;
                        rowNum++;
                        continue;
                    }

                    if (!email.matches("\\S+@\\S+\\.\\S+")) {
                        errors.add("Row " + rowNum + ": Invalid email: " + email);
                        failed++;
                        rowNum++;
                        continue;
                    }

                    // Skip duplicates — don't crash, just report
                    if (studentRepository.existsByEmail(email)) {
                        skipped++;
                        rowNum++;
                        continue;
                    }

                    Student student = Student.builder()
                            .name(name)
                            .email(email)
                            .phone(phone.isEmpty() ? null : phone)
                            .department(department)
                            .build();

                    batch.add(student);

                    // Save in batches of 500 — efficient for large files
                    if (batch.size() == BATCH_SIZE) {
                        studentRepository.saveAll(batch);
                        imported += batch.size();
                        batch.clear();
                        log.info("Imported {} students so far...", imported);
                    }

                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    failed++;
                }
                rowNum++;
            }

            // Save any remaining records in the last batch
            if (!batch.isEmpty()) {
                studentRepository.saveAll(batch);
                imported += batch.size();
            }

        } catch (Exception e) {
            errors.add("Failed to read file: " + e.getMessage());
        }

        return buildResult(totalRows, imported, skipped, failed, errors);
    }

    @Override
    public ImportResultDto importBooks(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0, imported = 0, skipped = 0, failed = 0;
        List<Book> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)))) {

            String[] headers = reader.readNext();
            // skip header row

            if (headers == null) {
                errors.add("CSV file is empty");
                return buildResult(0, 0, 0, 1, errors);
            }

            String[] row;
            int rowNum = 2;

            while ((row = reader.readNext()) != null) {
                totalRows++;

                try {
                    // CSV format: title, author, genre, isbn
                    if (row.length < 4) {
                        errors.add("Row " + rowNum + ": Not enough columns (expected 4)");
                        failed++;
                        rowNum++;
                        continue;
                    }

                    String title  = row[0].trim();
                    String author = row[1].trim();
                    String genre  = row[2].trim();
                    String isbn   = row[3].trim();

                    // Basic validation
                    if (title.isEmpty() || author.isEmpty() || genre.isEmpty() || isbn.isEmpty()) {
                        errors.add("Row " + rowNum + ": All fields are required");
                        failed++;
                        rowNum++;
                        continue;
                    }

                    if (!isbn.matches("^ISBN-[0-9]{3,}$")) {
                        errors.add("Row " + rowNum + ": Invalid ISBN format: " + isbn
                                + " (must be ISBN-001 style)");
                        failed++;
                        rowNum++;
                        continue;
                    }

                    // Skip duplicate ISBNs
                    if (bookRepository.existsByIsbn(isbn)) {
                        skipped++;
                        rowNum++;
                        continue;
                    }

                    Book book = Book.builder()
                            .title(title)
                            .author(author)
                            .genre(genre)
                            .isbn(isbn)
                            .student(null)
                            // all imported books start as unassigned
                            .build();

                    batch.add(book);

                    if (batch.size() == BATCH_SIZE) {
                        bookRepository.saveAll(batch);
                        imported += batch.size();
                        batch.clear();
                        log.info("Imported {} books so far...", imported);
                    }

                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    failed++;
                }
                rowNum++;
            }

            if (!batch.isEmpty()) {
                bookRepository.saveAll(batch);
                imported += batch.size();
            }

        } catch (Exception e) {
            errors.add("Failed to read file: " + e.getMessage());
        }

        return buildResult(totalRows, imported, skipped, failed, errors);
    }

    private ImportResultDto buildResult(int total, int imported,
                                        int skipped, int failed,
                                        List<String> errors) {
        // Only keep first 50 errors — don't flood the response
        List<String> trimmedErrors = errors.size() > 50
                ? errors.subList(0, 50)
                : errors;

        return ImportResultDto.builder()
                .totalRows(total)
                .imported(imported)
                .skipped(skipped)
                .failed(failed)
                .errors(trimmedErrors)
                .build();
    }
}