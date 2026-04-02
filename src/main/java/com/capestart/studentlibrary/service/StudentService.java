package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.request.StudentRequestDto;
import com.capestart.studentlibrary.dto.response.PageResponseDto;
import com.capestart.studentlibrary.dto.response.StudentResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    StudentResponseDto createStudent(StudentRequestDto requestDto);

    PageResponseDto<StudentResponseDto> getAllStudentsPaged(Pageable pageable);

    PageResponseDto<StudentResponseDto> searchStudents(String query, int page, int size);

    List<StudentResponseDto> getAllStudents();

    StudentResponseDto getStudentById(Long id);

    StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto);

    void deleteStudent(Long id);

    StudentResponseDto assignBookToStudent(Long studentId, Long bookId);

    StudentResponseDto removeBookFromStudent(Long studentId, Long bookId);
}