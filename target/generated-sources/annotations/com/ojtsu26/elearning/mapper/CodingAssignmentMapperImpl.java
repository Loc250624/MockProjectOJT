package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.CodingAssignmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CodingAssignmentResponseDTO;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Lesson;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T19:01:29+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class CodingAssignmentMapperImpl implements CodingAssignmentMapper {

    @Override
    public CodingAssignmentResponseDTO toDto(CodingAssignment entity) {
        if ( entity == null ) {
            return null;
        }

        CodingAssignmentResponseDTO codingAssignmentResponseDTO = new CodingAssignmentResponseDTO();

        codingAssignmentResponseDTO.setLessonId( entityLessonId( entity ) );
        codingAssignmentResponseDTO.setId( entity.getId() );
        codingAssignmentResponseDTO.setTitle( entity.getTitle() );
        codingAssignmentResponseDTO.setProblemStatement( entity.getProblemStatement() );
        codingAssignmentResponseDTO.setAllowedLanguages( entity.getAllowedLanguages() );
        codingAssignmentResponseDTO.setTimeLimitMs( entity.getTimeLimitMs() );

        return codingAssignmentResponseDTO;
    }

    @Override
    public CodingAssignment toEntity(CodingAssignmentRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        CodingAssignment.CodingAssignmentBuilder codingAssignment = CodingAssignment.builder();

        codingAssignment.lesson( codingAssignmentRequestDTOToLesson( dto ) );
        codingAssignment.title( dto.getTitle() );
        codingAssignment.problemStatement( dto.getProblemStatement() );
        codingAssignment.allowedLanguages( dto.getAllowedLanguages() );
        codingAssignment.timeLimitMs( dto.getTimeLimitMs() );

        return codingAssignment.build();
    }

    private Integer entityLessonId(CodingAssignment codingAssignment) {
        if ( codingAssignment == null ) {
            return null;
        }
        Lesson lesson = codingAssignment.getLesson();
        if ( lesson == null ) {
            return null;
        }
        Integer id = lesson.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected Lesson codingAssignmentRequestDTOToLesson(CodingAssignmentRequestDTO codingAssignmentRequestDTO) {
        if ( codingAssignmentRequestDTO == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.id( codingAssignmentRequestDTO.getLessonId() );

        return lesson.build();
    }
}
