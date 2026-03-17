package com.capestart.studentlibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponseDto {

    private Long id;
    private String title;
    private String author;
    private String genre;
    private String isbn;
    private Long studentId;
    private String studentName;
}