package com.ojtsu26.elearning.service.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGradingServiceTest {
    private final QuizGradingService service = new QuizGradingService(new ObjectMapper());

    @Test
    void gradesOnlyAssignedSetUsingPointsAndCorrectAnswerSnapshots() {
        Question liveQuestion = Question.builder()
                .id(10)
                .correctAnswer("B")
                .active(false)
                .build();
        QuizAttempt attempt = QuizAttempt.builder().id(20).build();
        QuizAttemptQuestion snapshot = QuizAttemptQuestion.builder()
                .attempt(attempt)
                .question(liveQuestion)
                .pointsSnapshot(new BigDecimal("3.50"))
                .optionsJsonSnapshot("[\"A\",\"B\"]")
                .correctAnswerSnapshot("A")
                .build();
        QuizAnswer answer = QuizAnswer.builder()
                .attempt(attempt)
                .question(liveQuestion)
                .answerText("A")
                .build();

        QuizGradingService.GradeResult result = service.grade(List.of(snapshot), List.of(answer));

        assertThat(result.earnedPoints()).isEqualByComparingTo("3.50");
        assertThat(result.totalPoints()).isEqualByComparingTo("3.50");
        assertThat(result.percentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void treatsMissingAnswerAsZeroAndSupportsSnapshotOptionIndexes() {
        Question first = Question.builder().id(10).build();
        Question second = Question.builder().id(11).build();
        QuizAttempt attempt = QuizAttempt.builder().id(20).build();
        List<QuizAttemptQuestion> assigned = List.of(
                snapshot(attempt, first, "0", "2.00"),
                snapshot(attempt, second, "1", "3.00"));
        QuizAnswer answer = QuizAnswer.builder()
                .attempt(attempt)
                .question(first)
                .selectedOptionsJson("[0]")
                .build();

        QuizGradingService.GradeResult result = service.grade(assigned, List.of(answer));

        assertThat(result.earnedPoints()).isEqualByComparingTo("2.00");
        assertThat(result.totalPoints()).isEqualByComparingTo("5.00");
        assertThat(result.percentage()).isEqualByComparingTo("40.00");
    }

    private QuizAttemptQuestion snapshot(
            QuizAttempt attempt, Question question, String correct, String points) {
        return QuizAttemptQuestion.builder()
                .attempt(attempt)
                .question(question)
                .pointsSnapshot(new BigDecimal(points))
                .optionsJsonSnapshot("[{\"content\":\"A\"},{\"content\":\"B\"}]")
                .correctAnswerSnapshot(correct)
                .build();
    }
}
