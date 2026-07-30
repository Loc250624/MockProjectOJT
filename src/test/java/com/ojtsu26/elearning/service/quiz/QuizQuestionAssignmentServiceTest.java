package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAttemptQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizQuestionAssignmentServiceTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuizAttemptQuestionRepository attemptQuestionRepository;

    private QuizQuestionAssignmentService service;
    private Quiz quiz;
    private QuizAttempt attempt;

    @BeforeEach
    void setUp() {
        service = new QuizQuestionAssignmentService(
                questionRepository,
                attemptQuestionRepository,
                new StratifiedQuestionSampler(new Random(17)));
        quiz = Quiz.builder().id(30).build();
        attempt = QuizAttempt.builder()
                .id(40)
                .quiz(quiz)
                .student(User.builder().id(50).build())
                .build();
        when(attemptQuestionRepository.findByAttemptIdOrderByDisplayOrderAsc(40))
                .thenReturn(List.of());
    }

    @Test
    void snapshotsExactlyTenUniqueQuestionsInStableDisplayOrder() {
        List<Question> bank = questions(35);
        when(questionRepository.findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
                30, QuestionReviewStatus.APPROVED)).thenReturn(bank);
        when(attemptQuestionRepository.findPreviouslySeenQuestionIds(50, 30))
                .thenReturn(Set.of());
        when(attemptQuestionRepository.countUsageByQuizId(30)).thenReturn(List.of());
        when(attemptQuestionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<QuizAttemptQuestion> result = service.assign(attempt);

        assertThat(result).hasSize(QuizRules.QUESTIONS_PER_ATTEMPT);
        assertThat(result).extracting(item -> item.getQuestion().getId()).doesNotHaveDuplicates();
        assertThat(result).extracting(QuizAttemptQuestion::getDisplayOrder)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(result).allSatisfy(item -> {
            assertThat(item.getQuestionTextSnapshot()).isNotBlank();
            assertThat(item.getOptionsJsonSnapshot()).isNotBlank();
            assertThat(item.getCorrectAnswerSnapshot()).isEqualTo("0");
        });
        assertThat(attempt.getTotalPoints()).isEqualByComparingTo("10");
    }

    @Test
    void rejectsAttemptBeforeWritingSnapshotWhenBankHasOnlyNineQuestions() {
        when(questionRepository.findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
                30, QuestionReviewStatus.APPROVED)).thenReturn(questions(9));

        assertThatThrownBy(() -> service.assign(attempt))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least 10 valid active questions");
        verify(attemptQuestionRepository, never()).saveAll(anyList());
    }

    private List<Question> questions(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> Question.builder()
                        .id(100 + index)
                        .quiz(quiz)
                        .questionText("Question " + index)
                        .optionsJson("[{\"content\":\"A\",\"correct\":true},"
                                + "{\"content\":\"B\",\"correct\":false}]")
                        .correctAnswer("0")
                        .questionType(QuestionType.SINGLE_CHOICE)
                        .points(BigDecimal.ONE)
                        .topicCode("GENERAL")
                        .difficulty(QuestionDifficulty.MEDIUM)
                        .reviewStatus(QuestionReviewStatus.APPROVED)
                        .active(true)
                        .version(1)
                        .build())
                .toList();
    }
}
