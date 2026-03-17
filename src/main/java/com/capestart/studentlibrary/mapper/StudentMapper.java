package com.capestart.studentlibrary.mapper;

import com.capestart.studentlibrary.dto.request.StudentRequestDto;
import com.capestart.studentlibrary.dto.response.StudentResponseDto;
import com.capestart.studentlibrary.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {BookMapper.class})
public interface StudentMapper {

    StudentResponseDto toResponseDto(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Student toEntity(StudentRequestDto studentRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntityFromDto(StudentRequestDto dto, @MappingTarget Student student);
}