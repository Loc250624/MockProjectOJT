package com.ojtsu26.elearning.feedback.email;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.StudentFeedbackRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.StudentFeedbackService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback_email_tx_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.main.allow-bean-definition-overriding=true",
        "app.feedback-email.enabled=true",
        "app.feedback-email.from=noreply@lumina.test",
        "app.feedback-email.reply-to=support@lumina.test",
        "app.feedback-email.support-email=support@lumina.test"
})
class FeedbackEmailTransactionalIntegrationTest {

    @MockBean
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Autowired
    private StudentFeedbackService feedbackService;

    @org.springframework.beans.factory.annotation.Autowired
    private StudentFeedbackRepository feedbackRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private TransactionTemplate transactionTemplate;

    private User student;

    @BeforeEach
    void setUp() {
        reset(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());

        feedbackRepository.deleteAll();
        userRepository.deleteAll();
        student = userRepository.save(testUser("Student One", "registered.student@example.com", Role.STUDENT));

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new CustomUserDetails(student),
                null,
                "ROLE_STUDENT"
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mailFailureAfterCommitDoesNotRemoveSavedFeedback() {
        doThrow(new MailSendException("smtp unavailable")).when(mailSender).send(any(MimeMessage.class));

        transactionTemplate.executeWithoutResult(status -> feedbackService.createForCurrentStudent(validRequest()));

        assertEquals(1, feedbackRepository.count());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void rollbackDoesNotSendFeedbackEmail() {
        transactionTemplate.executeWithoutResult(status -> {
            feedbackService.createForCurrentStudent(validRequest());
            status.setRollbackOnly();
        });

        assertEquals(0, feedbackRepository.count());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @TestConfiguration
    static class SynchronousFeedbackEmailExecutorConfig {
        @Bean(name = "feedbackEmailExecutor")
        Executor feedbackEmailExecutor() {
            return new ConcurrentTaskExecutor(Runnable::run);
        }
    }

    private StudentFeedbackRequestDTO validRequest() {
        StudentFeedbackRequestDTO request = new StudentFeedbackRequestDTO();
        request.setCategory(FeedbackCategory.PLATFORM_UI);
        request.setSubject("Platform idea");
        request.setContent("Please improve search.");
        request.setCourseContentRating(5);
        request.setInstructorSupportRating(4);
        request.setLearningExperienceRating(3);
        request.setPlatformUsabilityRating(2);
        request.setAssessmentExperienceRating(1);
        request.setOverallSatisfactionRating(5);
        return request;
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }
}
