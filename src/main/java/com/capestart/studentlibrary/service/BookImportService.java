package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.capestart.studentlibrary.entity.Book;
import com.capestart.studentlibrary.repository.BookRepository;
import com.capestart.studentlibrary.service.common.CsvBatchProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BookImportService {

    private final CsvBatchProcessor csvBatchProcessor;
    private final BookRepository bookRepository;

    private static final Pattern ISBN_PATTERN = Pattern.compile("^ISBN-[0-9]{3,}$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,:;!?()'&+]{2,}$");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^[a-zA-Z\\s'.\\-]{2,}$");
    private static final Pattern GENRE_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-]{2,}$");

    public ImportResultDto importBooks(MultipartFile file) {
        String[] expectedHeaders = {"title", "author", "genre", "isbn"};

        return csvBatchProcessor.processCsvFile(
                file,
                expectedHeaders,
                this::mapToBook,
                Book::getIsbn,
                bookRepository::findExistingIsbns,
                bookRepository
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
}