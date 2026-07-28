package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "course.id", target = "courseId")
    TransactionResponseDTO toDto(Transaction entity);

    @Mapping(source = "studentId", target = "student.id")
    @Mapping(source = "courseId", target = "course.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "order", ignore = true)
    Transaction toEntity(TransactionRequestDTO dto);
}
