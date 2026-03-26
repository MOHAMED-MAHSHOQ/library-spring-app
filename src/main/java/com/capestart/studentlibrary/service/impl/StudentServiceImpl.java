package com.capestart.studentlibrary.service.impl;

import com.capestart.studentlibrary.dto.request.StudentRequestDto;
import com.capestart.studentlibrary.dto.response.PageResponseDto;
import com.capestart.studentlibrary.dto.response.StudentResponseDto;
import com.capestart.studentlibrary.entity.Book;
import com.capestart.studentlibrary.entity.Student;
import com.capestart.studentlibrary.mapper.StudentMapper;
import com.capestart.studentlibrary.repository.BookRepository;
import com.capestart.studentlibrary.repository.StudentRepository;
import com.capestart.studentlibrary.service.StudentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final StudentMapper studentMapper;

    @Override
    public PageResponseDto<StudentResponseDto> getAllStudentsPaged(Pageable pageable) {
        var page = studentRepository.findAll(pageable);

        return PageResponseDto.<StudentResponseDto>builder()
                .content(page.getContent().stream()
                        .map(studentMapper::toResponseDto)
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
    public StudentResponseDto createStudent(StudentRequestDto requestDto) {
        if (studentRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException(
                    "Student with email " + requestDto.getEmail() + " already exists"
            );
        }
        Student student = studentMapper.toEntity(requestDto);
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toResponseDto(savedStudent);
    }

    @Override
    public List<StudentResponseDto> getAllStudents() {
        return studentRepository.findAllOrderById()
                .stream()
                .map(studentMapper::toResponseDto)
                .toList();
    }

    @Override
    public StudentResponseDto getStudentById(Long id) {
        Student student = findStudentById(id);
        return studentMapper.toResponseDto(student);
    }

    @Override
    public StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto) {
        Student existingStudent = findStudentById(id);
        if (!existingStudent.getEmail().equals(requestDto.getEmail()) &&
                studentRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException(
                    "Email " + requestDto.getEmail() + " is already taken"
            );
        }
        studentMapper.updateEntityFromDto(requestDto, existingStudent);
        Student updatedStudent = studentRepository.save(existingStudent);
        return studentMapper.toResponseDto(updatedStudent);
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = findStudentById(id);

        List<Book> assignedBooks = bookRepository.findByStudentIdOrderByIdAsc(id);
        assignedBooks.forEach(book -> book.setStudent(null));

        bookRepository.saveAll(assignedBooks);
        studentRepository.delete(student);
    }

    @Override
    public StudentResponseDto assignBookToStudent(Long studentId, Long bookId) {
        Student student = findStudentById(studentId);
        Book book = findBookById(bookId);
        if (book.getStudent() != null) {
            throw new IllegalArgumentException(
                    "Book '" + book.getTitle() + "' is already assigned to student '"
                            + book.getStudent().getName() + "'"
            );
        }
        book.setStudent(student);
        bookRepository.saveAndFlush(book);
        Student updatedStudent = findStudentById(studentId);
        return studentMapper.toResponseDto(updatedStudent);
    }

    @Override
    public StudentResponseDto removeBookFromStudent(Long studentId, Long bookId) {
        findStudentById(studentId);
        Book book = bookRepository.findByStudentIdAndBookId(studentId, bookId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Book with id " + bookId + " is not assigned to student with id " + studentId
                ));
        book.setStudent(null);
        bookRepository.saveAndFlush(book);
        Student updatedStudent = findStudentById(studentId);
        return studentMapper.toResponseDto(updatedStudent);
    }

    private Student findStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found with id: " + id
                ));
    }

    private Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Book not found with id: " + id
                ));
    }
}