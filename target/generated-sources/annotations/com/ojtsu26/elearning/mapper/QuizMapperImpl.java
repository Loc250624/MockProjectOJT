package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.QuizRequestDTO;
import com.ojtsu26.elearning.dto.response.QuizResponseDTO;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Quiz;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T21:10:47+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class QuizMapperImpl implements QuizMapper {

    @Override
    public QuizResponseDTO toDto(Quiz entity) {
        if ( entity == null ) {
            return null;
        }

        QuizResponseDTO quizResponseDTO = new QuizResponseDTO();

        quizResponseDTO.setLessonId( entityLessonId( entity ) );
        quizResponseDTO.setId( entity.getId() );
        quizResponseDTO.setTitle( entity.getTitle() );
        quizResponseDTO.setPassingScore( entity.getPassingScore() );

        return quizResponseDTO;
    }

    @Override
    public Quiz toEntity(QuizRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Quiz.QuizBuilder quiz = Quiz.builder();

        quiz.lesson( quizRequestDTOToLesson( dto ) );
        quiz.title( dto.getTitle() );
        quiz.passingScore( dto.getPassingScore() );

        return quiz.build();
    }

    private Integer entityLessonId(Quiz quiz) {
        if ( quiz == null ) {
            return null;
        }
        Lesson lesson = quiz.getLesson();
        if ( lesson == null ) {
            return null;
        }
        Integer id = lesson.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected Lesson quizRequestDTOToLesson(QuizRequestDTO quizRequestDTO) {
        if ( quizRequestDTO == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.id( quizRequestDTO.getLessonId() );

        return lesson.build();
    }
}
