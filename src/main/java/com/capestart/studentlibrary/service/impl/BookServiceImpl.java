package com.capestart.studentlibrary.service.impl;

import com.capestart.studentlibrary.dto.request.BookRequestDto;
import com.capestart.studentlibrary.dto.response.BookResponseDto;
import com.capestart.studentlibrary.dto.response.PageResponseDto;
import com.capestart.studentlibrary.entity.Book;
import com.capestart.studentlibrary.mapper.BookMapper;
import com.capestart.studentlibrary.repository.BookRepository;
import com.capestart.studentlibrary.service.BookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Override
    public PageResponseDto<BookResponseDto> getAllBooksPaged(Pageable pageable) {
        var page = bookRepository.findAll(pageable);
        return PageResponseDto.<BookResponseDto>builder()
                .content(page.getContent().stream()
                        .map(bookMapper::toResponseDto)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    public BookResponseDto createBook(BookRequestDto requestDto) {
        if (bookRepository.existsByIsbn(requestDto.getIsbn())) {
            throw new IllegalArgumentException(
                    "Book with ISBN " + requestDto.getIsbn() + " already exists"
            );
        }
        Book book = bookMapper.toEntity(requestDto);
        Book savedBook = bookRepository.save(book);
        return bookMapper.toResponseDto(savedBook);
    }

    @Override
    public List<BookResponseDto> getAllBooks() {
        return bookRepository.findAllOrderById()
                .stream()
                .map(bookMapper::toResponseDto)
                .toList();
    }

    @Override
    public BookResponseDto getBookById(Long id) {
        Book book = findBookById(id);
        return bookMapper.toResponseDto(book);
    }

    @Override
    public BookResponseDto updateBook(Long id, BookRequestDto requestDto) {
        Book existingBook = findBookById(id);
        if (!existingBook.getIsbn().equals(requestDto.getIsbn()) &&
                bookRepository.existsByIsbn(requestDto.getIsbn())) {
            throw new IllegalArgumentException(
                    "ISBN " + requestDto.getIsbn() + " already exists"
            );
        }
        bookMapper.updateEntityFromDto(requestDto, existingBook);
        Book updatedBook = bookRepository.save(existingBook);
        return bookMapper.toResponseDto(updatedBook);
    }

    @Override
    public void deleteBook(Long id) {
        Book book = findBookById(id);
        if(book.getStudent() != null) {
            throw new IllegalStateException(
                    "Cannot delete book with id " + id + " because it is assigned to a student"
            );
        }
        bookRepository.delete(book);
    }

    @Override
    public List<BookResponseDto> getUnassignedBooks() {
        return bookRepository.findByStudentIsNull()
                .stream()
                .map(bookMapper::toResponseDto)
                .toList();
    }

    private Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Book not found with id: " + id
                ));
    }
}