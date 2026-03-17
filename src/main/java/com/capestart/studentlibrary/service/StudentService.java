package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.request.StudentRequestDto;
import com.capestart.studentlibrary.dto.response.StudentResponseDto;

import java.util.List;

public interface StudentService {

    StudentResponseDto createStudent(StudentRequestDto requestDto);

    List<StudentResponseDto> getAllStudents();

    StudentResponseDto getStudentById(Long id);

    StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto);

    void deleteStudent(Long id);

    StudentResponseDto assignBookToStudent(Long studentId, Long bookId);

    StudentResponseDto removeBookFromStudent(Long studentId, Long bookId);
}