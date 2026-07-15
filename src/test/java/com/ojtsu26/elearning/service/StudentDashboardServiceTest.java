package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.StudentDeadlineDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.projection.StudentDeadlineProjection;
import com.ojtsu26.elearning.service.impl.StudentDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CodingAssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    private StudentDashboardServiceImpl service;
    private User student;

    @BeforeEach
    void setUp() {
        service = new StudentDashboardServiceImpl(currentUserService, assignmentRepository, submissionRepository);
        student = User.builder().id(5).role(Role.STUDENT).status(UserStatus.ACTIVE).build();
    }

    @Test
    void returnsScopedDeadlinesAndSubmissionStatus() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(submissionRepository.findCompletedAssignmentIdsForStudent(eq(5), any(Collection.class))).thenReturn(List.of(99));
        LocalDateTime dueSoon = LocalDateTime.now().plusDays(2);
        when(assignmentRepository.findUpcomingDeadlinesForStudentExcludingCompleted(eq(5), eq(List.of(99)), any(Pageable.class)))
                .thenReturn(List.of(deadline(10, "Lab", 20, "Algorithms", dueSoon)));
        CodingAssignment assignment = CodingAssignment.builder().id(10).build();
        Submission submission = Submission.builder()
                .assignment(assignment)
                .status(SubmissionStatus.PENDING_REVIEW)
                .build();
        when(submissionRepository.findLatestByAssignmentIdsAndStudentId(List.of(10), 5))
                .thenReturn(List.of(submission));

        List<StudentDeadlineDashboardDTO> result = service.getCurrentStudentDeadlines(3);

        assertEquals(1, result.size());
        assertEquals("Lab", result.get(0).getTitle());
        assertEquals("Algorithms", result.get(0).getCourseTitle());
        assertEquals("Submitted", result.get(0).getStatusLabel());
        assertEquals("/student/assignments/10/submit", result.get(0).getDestinationUrl());
        verify(assignmentRepository).findUpcomingDeadlinesForStudentExcludingCompleted(eq(5), eq(List.of(99)), any(Pageable.class));
    }

    @Test
    void usesSentinelWhenStudentHasNoCompletedAssignments() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(submissionRepository.findCompletedAssignmentIdsForStudent(eq(5), any(Collection.class))).thenReturn(List.of());
        when(assignmentRepository.findUpcomingDeadlinesForStudentExcludingCompleted(eq(5), any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.getCurrentStudentDeadlines(3);

        ArgumentCaptor<Collection<Integer>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(assignmentRepository).findUpcomingDeadlinesForStudentExcludingCompleted(eq(5), idsCaptor.capture(), any(Pageable.class));
        assertEquals(Set.of(-1), Set.copyOf(idsCaptor.getValue()));
        verify(submissionRepository, never()).findLatestByAssignmentIdsAndStudentId(any(), any());
    }

    @Test
    void nonStudentCannotReadStudentDashboardData() {
        User teacher = User.builder().id(7).role(Role.TEACHER).status(UserStatus.ACTIVE).build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);

        assertThrows(BusinessException.class, () -> service.getCurrentStudentDeadlines(3));

        verifyNoInteractions(assignmentRepository, submissionRepository);
    }

    private StudentDeadlineProjection deadline(Integer assignmentId,
                                               String title,
                                               Integer courseId,
                                               String courseTitle,
                                               LocalDateTime dueAt) {
        return new StudentDeadlineProjection() {
            @Override
            public Integer getAssignmentId() {
                return assignmentId;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public String getCourseTitle() {
                return courseTitle;
            }

            @Override
            public LocalDateTime getDueAt() {
                return dueAt;
            }
        };
    }
}
