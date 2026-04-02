package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import com.capestart.studentlibrary.entity.Student;
import com.capestart.studentlibrary.repository.StudentRepository;
import com.capestart.studentlibrary.service.common.CsvBatchProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final CsvBatchProcessor csvBatchProcessor;
    private final StudentRepository studentRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    public ImportResultDto importStudents(MultipartFile file) {
        String[] expectedHeaders = {"name", "email", "phone", "department"};

        return csvBatchProcessor.processCsvFile(
                file,
                expectedHeaders,
                this::mapToStudent,
                Student::getEmail,
                studentRepository::findExistingEmails,
                studentRepository
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
}