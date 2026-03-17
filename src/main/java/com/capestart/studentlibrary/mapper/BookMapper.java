package com.capestart.studentlibrary.mapper;

import com.capestart.studentlibrary.dto.request.BookRequestDto;
import com.capestart.studentlibrary.dto.response.BookResponseDto;
import com.capestart.studentlibrary.dto.response.BookSummaryDto;
import com.capestart.studentlibrary.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.name")
    BookResponseDto toResponseDto(Book book);

    BookSummaryDto toSummaryDto(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    Book toEntity(BookRequestDto bookRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    void updateEntityFromDto(BookRequestDto dto, @MappingTarget Book book);
}