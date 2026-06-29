package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.dto.request.SubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.SubmissionResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "lesson.id", target = "lessonId")
    SubmissionResponseDTO toDto(Submission entity);

    @Mapping(source = "studentId", target = "student.id")
    @Mapping(source = "lessonId", target = "lesson.id")
    Submission toEntity(SubmissionRequestDTO dto);
}
