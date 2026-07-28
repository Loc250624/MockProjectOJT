package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quiz_question_repository_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TeacherAssessmentRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void randomQuizQuestionQueryReturnsTenQuestionsFromRequestedQuiz() {
        User teacher = persistUser("teacher.quiz@example.com");
        Quiz javaQuiz = persistQuiz("Java OOP", teacher);
        Quiz otherQuiz = persistQuiz("SQL", teacher);
        persistQuestions(javaQuiz, 100);
        persistQuestions(otherQuiz, 12);
        entityManager.flush();
        entityManager.clear();

        var selected = questionRepository.findRandomByQuizId(
                javaQuiz.getId(), PageRequest.of(0, 10));

        assertThat(selected).hasSize(10);
        assertThat(selected)
                .allMatch(question -> question.getQuiz().getId().equals(javaQuiz.getId()));
    }

    @Test
    void randomQuizQuestionQueryReturnsAllWhenQuizHasFewerThanTen() {
        User teacher = persistUser("teacher.small@example.com");
        Quiz quiz = persistQuiz("Small Quiz", teacher);
        persistQuestions(quiz, 7);
        entityManager.flush();
        entityManager.clear();

        var selected = questionRepository.findRandomByQuizId(
                quiz.getId(), PageRequest.of(0, 10));

        assertThat(selected).hasSize(7);
    }

    private User persistUser(String email) {
        User user = User.builder()
                .fullName("Quiz Teacher")
                .email(email)
                .passwordHash("secret")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Quiz persistQuiz(String title, User teacher) {
        Course course = Course.builder()
                .title(title)
                .description(title)
                .price(BigDecimal.ZERO)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build();
        entityManager.persist(course);
        Lesson lesson = Lesson.builder()
                .title(title + " Quiz")
                .type(LessonType.QUIZ)
                .course(course)
                .orderIndex(1)
                .build();
        entityManager.persist(lesson);
        Quiz quiz = Quiz.builder()
                .title(title + " Quiz")
                .lesson(lesson)
                .createdBy(teacher)
                .build();
        entityManager.persist(quiz);
        return quiz;
    }

    private void persistQuestions(Quiz quiz, int count) {
        for (int index = 1; index <= count; index++) {
            entityManager.persist(Question.builder()
                    .quiz(quiz)
                    .questionText("Question " + index)
                    .optionsJson("[\"A\",\"B\"]")
                    .correctAnswer("A")
                    .displayOrder(index)
                    .build());
        }
    }
}
