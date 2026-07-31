package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentFeedbackResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.feedback.email.FeedbackSubmittedEvent;
import com.ojtsu26.elearning.model.entity.StudentFeedback;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.StudentFeedbackRepository;
import com.ojtsu26.elearning.service.impl.StudentFeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentFeedbackServiceTest {

    @Mock
    private StudentFeedbackRepository feedbackRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StudentFeedbackServiceImpl service;
    private User student;
    private User admin;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new StudentFeedbackServiceImpl(feedbackRepository, currentUserService, eventPublisher);
        student = User.builder().id(1).fullName("Student One").email("student@example.com").role(Role.STUDENT).build();
        admin = User.builder().id(2).fullName("Admin One").email("admin@example.com").role(Role.ADMIN).build();
        teacher = User.builder().id(3).fullName("Teacher One").email("teacher@example.com").role(Role.TEACHER).build();
    }

    @Test
    void createAssignsCurrentStudentAndTrimsInput() {
        StudentFeedbackRequestDTO request = validRequest();
        request.setSubject("  Platform idea  ");
        request.setContent("  Please improve search.  ");

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(feedbackRepository.save(any(StudentFeedback.class))).thenAnswer(invocation -> {
            StudentFeedback feedback = invocation.getArgument(0);
            feedback.setId(10);
            feedback.setCreatedAt(LocalDateTime.of(2026, 7, 22, 9, 30));
            return feedback;
        });

        StudentFeedbackResponseDTO response = service.createForCurrentStudent(request);

        ArgumentCaptor<StudentFeedback> captor = ArgumentCaptor.forClass(StudentFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        StudentFeedback saved = captor.getValue();
        assertEquals(student, saved.getStudent());
        assertEquals(FeedbackCategory.PLATFORM_UI, saved.getCategory());
        assertEquals("Platform idea", saved.getSubject());
        assertEquals("Please improve search.", saved.getContent());
        assertEquals(5, saved.getCourseContentRating());
        assertEquals(4, saved.getInstructorSupportRating());
        assertEquals(3, saved.getLearningExperienceRating());
        assertEquals(2, saved.getPlatformUsabilityRating());
        assertEquals(1, saved.getAssessmentExperienceRating());
        assertEquals(5, saved.getOverallSatisfactionRating());
        assertEquals(1, response.getStudentId());
        assertEquals("Platform/UI", response.getCategoryLabel());

        ArgumentCaptor<FeedbackSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(FeedbackSubmittedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        FeedbackSubmittedEvent event = eventCaptor.getValue();
        assertEquals(10, event.feedbackId());
        assertEquals("Student One", event.studentDisplayName());
        assertEquals("student@example.com", event.studentEmail());
        assertEquals("Platform idea", event.subject());
        assertEquals("Please improve search.", event.content());
        assertEquals(5, event.courseContentRating());
        assertEquals(4, event.instructorSupportRating());
        assertEquals(3, event.learningExperienceRating());
        assertEquals(2, event.platformUsabilityRating());
        assertEquals(1, event.assessmentExperienceRating());
        assertEquals(5, event.overallSatisfactionRating());
        assertEquals(LocalDateTime.of(2026, 7, 22, 9, 30), event.submittedAt());
    }

    @Test
    void createPublishesPostCommitEventOnceUsingStoredStudentEmail() {
        StudentFeedbackRequestDTO request = validRequest();
        student.setEmail("registered.student@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(feedbackRepository.save(any(StudentFeedback.class))).thenAnswer(invocation -> {
            StudentFeedback feedback = invocation.getArgument(0);
            feedback.setId(11);
            return feedback;
        });

        service.createForCurrentStudent(request);

        ArgumentCaptor<FeedbackSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(FeedbackSubmittedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        FeedbackSubmittedEvent event = eventCaptor.getValue();
        assertEquals(11, event.feedbackId());
        assertEquals("registered.student@example.com", event.studentEmail());
        assertNotNull(event.submittedAt());
    }

    @Test
    void createRejectsNonStudentBeforeSaving() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);

        assertThrows(BusinessException.class, () -> service.createForCurrentStudent(validRequest()));

        verify(feedbackRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createDefaultsMissingCategoryForBackwardCompatibility() {
        StudentFeedbackRequestDTO request = validRequest();
        request.setCategory(null);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(feedbackRepository.save(any(StudentFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentFeedbackResponseDTO response = service.createForCurrentStudent(request);

        ArgumentCaptor<StudentFeedback> captor = ArgumentCaptor.forClass(StudentFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals(FeedbackCategory.OTHER, captor.getValue().getCategory());
        assertEquals(FeedbackCategory.OTHER, response.getCategory());
        assertEquals("Other", response.getCategoryLabel());
    }

    @Test
    void createRejectsInvalidRatingBeforeSaving() {
        StudentFeedbackRequestDTO request = validRequest();
        request.setOverallSatisfactionRating(6);
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.createForCurrentStudent(request));

        verify(feedbackRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void adminListUsesGlobalRepositoryQuery() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(feedback("Admin visible", "Content"))));

        assertEquals(1, service.getAllFeedbacksForAdmin(0, 10).getContent().size());

        verify(feedbackRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void adminDetailUsesGlobalRepositoryQuery() {
        StudentFeedback feedback = feedback("Detail", "Visible to admin");
        feedback.setId(30);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(feedbackRepository.findWithStudentById(30)).thenReturn(Optional.of(feedback));

        StudentFeedbackResponseDTO response = service.getFeedbackDetailForAdmin(30);

        assertEquals("Detail", response.getSubject());
        verify(feedbackRepository).findWithStudentById(30);
    }

    @Test
    void adminDetailRejectsStudentRole() {
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.getFeedbackDetailForAdmin(30));

        verify(feedbackRepository, never()).findWithStudentById(any());
    }

    @Test
    void adminListRejectsInvalidPageSize() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        assertThrows(BusinessException.class, () -> service.getAllFeedbacksForAdmin(0, 99));

        verify(feedbackRepository, never()).findAllByOrderByCreatedAtDesc(any());
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

    private StudentFeedback feedback(String subject, String content) {
        return StudentFeedback.builder()
                .student(student)
                .category(FeedbackCategory.COURSE_CONTENT)
                .subject(subject)
                .content(content)
                .courseContentRating(5)
                .instructorSupportRating(5)
                .learningExperienceRating(4)
                .platformUsabilityRating(4)
                .assessmentExperienceRating(3)
                .overallSatisfactionRating(5)
                .build();
    }
}
