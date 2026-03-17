package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.request.BookRequestDto;
import com.capestart.studentlibrary.dto.response.BookResponseDto;

import java.util.List;

public interface BookService {

    BookResponseDto createBook(BookRequestDto requestDto);

    List<BookResponseDto> getAllBooks();

    BookResponseDto getBookById(Long id);

    BookResponseDto updateBook(Long id, BookRequestDto requestDto);

    void deleteBook(Long id);

    List<BookResponseDto> getUnassignedBooks();
}