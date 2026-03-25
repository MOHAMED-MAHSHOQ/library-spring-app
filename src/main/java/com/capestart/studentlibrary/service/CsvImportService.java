package com.capestart.studentlibrary.service;

import com.capestart.studentlibrary.dto.response.ImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface CsvImportService {
    ImportResultDto importStudents(MultipartFile file);
    ImportResultDto importBooks(MultipartFile file);
}