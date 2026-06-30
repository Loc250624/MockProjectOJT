package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.SubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.SubmissionResponseDTO;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T21:10:47+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class SubmissionMapperImpl implements SubmissionMapper {

    @Override
    public SubmissionResponseDTO toDto(Submission entity) {
        if ( entity == null ) {
            return null;
        }

        SubmissionResponseDTO submissionResponseDTO = new SubmissionResponseDTO();

        submissionResponseDTO.setStudentId( entityStudentId( entity ) );
        submissionResponseDTO.setLessonId( entityLessonId( entity ) );
        submissionResponseDTO.setId( entity.getId() );
        submissionResponseDTO.setScore( entity.getScore() );
        submissionResponseDTO.setStatus( entity.getStatus() );
        submissionResponseDTO.setSubmittedContent( entity.getSubmittedContent() );
        submissionResponseDTO.setTeacherFeedback( entity.getTeacherFeedback() );
        submissionResponseDTO.setSubmittedAt( entity.getSubmittedAt() );

        return submissionResponseDTO;
    }

    @Override
    public Submission toEntity(SubmissionRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Submission.SubmissionBuilder submission = Submission.builder();

        submission.student( submissionRequestDTOToUser( dto ) );
        submission.lesson( submissionRequestDTOToLesson( dto ) );
        submission.score( dto.getScore() );
        submission.status( dto.getStatus() );
        submission.submittedContent( dto.getSubmittedContent() );
        submission.teacherFeedback( dto.getTeacherFeedback() );

        return submission.build();
    }

    private Integer entityStudentId(Submission submission) {
        if ( submission == null ) {
            return null;
        }
        User student = submission.getStudent();
        if ( student == null ) {
            return null;
        }
        Integer id = student.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityLessonId(Submission submission) {
        if ( submission == null ) {
            return null;
        }
        Lesson lesson = submission.getLesson();
        if ( lesson == null ) {
            return null;
        }
        Integer id = lesson.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User submissionRequestDTOToUser(SubmissionRequestDTO submissionRequestDTO) {
        if ( submissionRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( submissionRequestDTO.getStudentId() );

        return user.build();
    }

    protected Lesson submissionRequestDTOToLesson(SubmissionRequestDTO submissionRequestDTO) {
        if ( submissionRequestDTO == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.id( submissionRequestDTO.getLessonId() );

        return lesson.build();
    }
}
