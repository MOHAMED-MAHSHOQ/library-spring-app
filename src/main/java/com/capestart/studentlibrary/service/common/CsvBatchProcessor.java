package com.capestart.studentlibrary.service.common;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CsvBatchProcessor {

    private static final int BATCH_SIZE = 500;
//seperate class
    public interface RowMapper<T> {
        T mapRow(String[] row) throws Exception;
    }

    private static class ImportStats {
        int totalRows = 0, imported = 0, skipped = 0, failed = 0;
        List<String> errors = new ArrayList<>();
    }

    public <T> ImportResultDto processCsvFile(
            MultipartFile file,
            String[] expectedHeaders,
            RowMapper<T> rowMapper,
            Function<T, String> keyExtractor,
            Function<Set<String>, Set<String>> duplicateChecker,
            JpaRepository<T, Long> repository) {

        ImportStats stats = new ImportStats();
        List<T> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)))) {

            String[] actualHeaders = reader.readNext();

            if (actualHeaders == null) {
                stats.errors.add("CSV file is completely empty");
                stats.failed = 1;
                return buildResult(stats);
            }

            if (actualHeaders.length < expectedHeaders.length) {
                stats.errors.add("Invalid CSV format. Missing columns. Expected: " + String.join(", ", expectedHeaders));
                stats.failed = 1;
                return buildResult(stats);
            }

            for (int i = 0; i < expectedHeaders.length; i++) {
                if (!actualHeaders[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                    stats.errors.add("Invalid CSV header at column " + (i + 1) + ". Expected '" + expectedHeaders[i] + "' but found '" + actualHeaders[i].trim() + "'.");
                    stats.failed = 1;
                    return buildResult(stats);
                }
            }

            String[] row;
            int rowNum = 2;
            int expectedCols = expectedHeaders.length;

            while ((row = reader.readNext()) != null) {
                stats.totalRows++;
                try {
                    if (row.length < expectedCols) throw new IllegalArgumentException("Not enough columns");

                    T entity = rowMapper.mapRow(row);
                    batch.add(entity);

                    if (batch.size() == BATCH_SIZE) {
                        saveBatchWithoutDuplicates(batch, keyExtractor, duplicateChecker, repository, stats);
                    }
                } catch (Exception e) {
                    stats.errors.add("Row " + rowNum + ": " + e.getMessage());
                    stats.failed++;
                }
                rowNum++;
            }

            if (!batch.isEmpty()) {
                saveBatchWithoutDuplicates(batch, keyExtractor, duplicateChecker, repository, stats);
            }

        } catch (Exception e) {
            stats.errors.add("Failed to read file: " + e.getMessage());
        }

        return buildResult(stats);
    }

    private <T> void saveBatchWithoutDuplicates(
            List<T> batch,
            Function<T, String> keyExtractor,
            Function<Set<String>, Set<String>> duplicateChecker,
            JpaRepository<T, Long> repository,
            ImportStats stats) {

        if (batch.isEmpty()) return;

        Set<String> keysInBatch = batch.stream().map(keyExtractor).collect(Collectors.toSet());
        Set<String> existingKeys = duplicateChecker.apply(keysInBatch);

        List<T> uniqueEntities = batch.stream()
                .filter(entity -> !existingKeys.contains(keyExtractor.apply(entity)))
                .collect(Collectors.toList());

        int duplicatesFound = batch.size() - uniqueEntities.size();
        stats.skipped += duplicatesFound;

        if (!uniqueEntities.isEmpty()) {
            repository.saveAll(uniqueEntities);
            stats.imported += uniqueEntities.size();
        }

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