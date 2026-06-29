package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.QuestionRequestDTO;
import com.ojtsu26.elearning.dto.response.QuestionResponseDTO;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T19:01:29+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class QuestionMapperImpl implements QuestionMapper {

    @Override
    public QuestionResponseDTO toDto(Question entity) {
        if ( entity == null ) {
            return null;
        }

        QuestionResponseDTO questionResponseDTO = new QuestionResponseDTO();

        questionResponseDTO.setQuizId( entityQuizId( entity ) );
        questionResponseDTO.setId( entity.getId() );
        questionResponseDTO.setQuestionText( entity.getQuestionText() );
        questionResponseDTO.setOptionsJson( entity.getOptionsJson() );
        questionResponseDTO.setCorrectAnswer( entity.getCorrectAnswer() );

        return questionResponseDTO;
    }

    @Override
    public Question toEntity(QuestionRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Question.QuestionBuilder question = Question.builder();

        question.quiz( questionRequestDTOToQuiz( dto ) );
        question.questionText( dto.getQuestionText() );
        question.optionsJson( dto.getOptionsJson() );
        question.correctAnswer( dto.getCorrectAnswer() );

        return question.build();
    }

    private Integer entityQuizId(Question question) {
        if ( question == null ) {
            return null;
        }
        Quiz quiz = question.getQuiz();
        if ( quiz == null ) {
            return null;
        }
        Integer id = quiz.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected Quiz questionRequestDTOToQuiz(QuestionRequestDTO questionRequestDTO) {
        if ( questionRequestDTO == null ) {
            return null;
        }

        Quiz.QuizBuilder quiz = Quiz.builder();

        quiz.id( questionRequestDTO.getQuizId() );

        return quiz.build();
    }
}
