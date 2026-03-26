package com.capestart.studentlibrary.service.impl;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.capestart.studentlibrary.entity.Book;
import com.capestart.studentlibrary.entity.Student;
import com.capestart.studentlibrary.repository.BookRepository;
import com.capestart.studentlibrary.repository.StudentRepository;
import com.capestart.studentlibrary.service.CsvImportService;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportServiceImpl implements CsvImportService {

    private static final int BATCH_SIZE = 500;

    // Strict Regex Patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ISBN_PATTERN = Pattern.compile("^ISBN-[0-9]{3,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,:;!?()'&+]{2,}$");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^[a-zA-Z\\s'.\\-]{2,}$");
    private static final Pattern GENRE_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-]{2,}$");

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;

    // --- INTERFACES & HELPER CLASSES ---

    // Defines how to convert a CSV String[] into a Java Entity
    private interface RowMapper<T> {
        T mapRow(String[] row) throws Exception;
    }

    // Holds our tracking numbers so the generic method can update them
    private static class ImportStats {
        int totalRows = 0, imported = 0, skipped = 0, failed = 0;
        List<String> errors = new ArrayList<>();
    }

    // ==========================================
    // 1. IMPORT STUDENTS
    // ==========================================
    @Override
    public ImportResultDto importStudents(MultipartFile file) {
        return processCsvFile(
                file,
                4,
                this::mapToStudent,                  // 1. How to map the row
                Student::getEmail,                   // 2. How to get the unique key (email)
                studentRepository::findExistingEmails, // 3. DB method to find duplicates
                studentRepository                    // 4. Where to save them
        );
    }

    private Student mapToStudent(String[] row) {
        String name = row[0].trim();
        String email = row[1].trim();
        String phone = row[2].trim();
        String department = row[3].trim();

        if (name.isEmpty() || email.isEmpty() || department.isEmpty()) throw new IllegalArgumentException("Name, email, and department are required");
        if (!NAME_PATTERN.matcher(name).matches()) throw new IllegalArgumentException("Invalid name format: " + name);
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new IllegalArgumentException("Invalid email format: " + email);
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) throw new IllegalArgumentException("Invalid phone: " + phone);

        return Student.builder().name(name).email(email).phone(phone.isEmpty() ? null : phone).department(department).build();
    }

    // ==========================================
    // 2. IMPORT BOOKS
    // ==========================================
    @Override
    public ImportResultDto importBooks(MultipartFile file) {
        return processCsvFile(
                file,
                4,
                this::mapToBook,                   // 1. How to map the row
                Book::getIsbn,                     // 2. How to get the unique key (ISBN)
                bookRepository::findExistingIsbns, // 3. DB method to find duplicates
                bookRepository                     // 4. Where to save them
        );
    }

    private Book mapToBook(String[] row) {
        String title = row[0].trim();
        String author = row[1].trim();
        String genre = row[2].trim();
        String isbn = row[3].trim();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty() || isbn.isEmpty()) throw new IllegalArgumentException("All fields are required");
        if (!TITLE_PATTERN.matcher(title).matches()) throw new IllegalArgumentException("Invalid title format: " + title);
        if (!AUTHOR_PATTERN.matcher(author).matches()) throw new IllegalArgumentException("Invalid author format: " + author);
        if (!GENRE_PATTERN.matcher(genre).matches()) throw new IllegalArgumentException("Invalid genre format: " + genre);
        if (!ISBN_PATTERN.matcher(isbn).matches()) throw new IllegalArgumentException("Invalid ISBN format: " + isbn);

        return Book.builder().title(title).author(author).genre(genre).isbn(isbn).student(null).build();
    }

    // ==========================================
    // 3. THE GENERIC CSV PROCESSOR (INDUSTRY STANDARD)
    // ==========================================
    private <T> ImportResultDto processCsvFile(
            MultipartFile file,
            int expectedCols,
            RowMapper<T> rowMapper,
            Function<T, String> keyExtractor,
            Function<Set<String>, Set<String>> duplicateChecker,
            JpaRepository<T, Long> repository) {

        ImportStats stats = new ImportStats();
        List<T> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)))) {

            if (reader.readNext() == null) {
                stats.errors.add("CSV file is empty");
                return buildResult(stats);
            }

            String[] row;
            int rowNum = 2;

            while ((row = reader.readNext()) != null) {
                stats.totalRows++;
                try {
                    if (row.length < expectedCols) throw new IllegalArgumentException("Not enough columns");

                    T entity = rowMapper.mapRow(row);
                    batch.add(entity);

                    // When batch is full, process duplicates and save
                    if (batch.size() == BATCH_SIZE) {
                        saveBatchWithoutDuplicates(batch, keyExtractor, duplicateChecker, repository, stats);
                    }
                } catch (Exception e) {
                    stats.errors.add("Row " + rowNum + ": " + e.getMessage());
                    stats.failed++;
                }
                rowNum++;
            }

            // Process the final remaining rows
            if (!batch.isEmpty()) {
                saveBatchWithoutDuplicates(batch, keyExtractor, duplicateChecker, repository, stats);
            }

        } catch (Exception e) {
            stats.errors.add("Failed to read file: " + e.getMessage());
        }

        return buildResult(stats);
    }

    // --- BATCH DUPLICATE FILTERING LOGIC ---
    private <T> void saveBatchWithoutDuplicates(
            List<T> batch,
            Function<T, String> keyExtractor,
            Function<Set<String>, Set<String>> duplicateChecker,
            JpaRepository<T, Long> repository,
            ImportStats stats) {

        if (batch.isEmpty()) return;

        // 1. Extract all unique keys (Emails/ISBNs) from the current batch
        Set<String> keysInBatch = batch.stream()
                .map(keyExtractor)
                .collect(Collectors.toSet());

        // 2. Ask the DB ONCE which of these keys already exist
        Set<String> existingKeys = duplicateChecker.apply(keysInBatch);

        // 3. Filter the batch in memory (Keep only the ones NOT in the DB)
        List<T> uniqueEntities = batch.stream()
                .filter(entity -> !existingKeys.contains(keyExtractor.apply(entity)))
                .collect(Collectors.toList());

        // 4. Update statistics
        int duplicatesFound = batch.size() - uniqueEntities.size();
        stats.skipped += duplicatesFound;

        // 5. Save only the unique entities to the database
        if (!uniqueEntities.isEmpty()) {
            repository.saveAll(uniqueEntities);
            stats.imported += uniqueEntities.size();
        }

        // 6. Empty the batch for the next round
        batch.clear();
    }

    private ImportResultDto buildResult(ImportStats stats) {
        return ImportResultDto.builder()
                .totalRows(stats.totalRows)
                .imported(stats.imported)
                .skipped(stats.skipped)
                .failed(stats.failed)
                .errors(stats.errors.size() > 50 ? stats.errors.subList(0, 50) : stats.errors)
                .build();
    }
}