package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.service.impl.StudentLearningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLearningServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CertificateService certificateService;

    private StudentLearningServiceImpl service;
    private User student;
    private Course course;
    private CourseEnrollment enrollment;
    private Lesson firstLesson;
    private Lesson secondLesson;

    @BeforeEach
    void setUp() {
        service = new StudentLearningServiceImpl(
                courseRepository,
                enrollmentRepository,
                lessonRepository,
                lessonProgressRepository,
                currentUserService,
                notificationService,
                certificateService
        );
        student = User.builder().id(1).role(Role.STUDENT).status(UserStatus.ACTIVE).build();
        course = Course.builder().id(10).title("Secure Learning").status(CourseStatus.APPROVED).build();
        enrollment = CourseEnrollment.builder().id(20).student(student).course(course).isCompleted(false).build();
        firstLesson = Lesson.builder()
                .id(101)
                .course(course)
                .title("Text Lesson")
                .type(LessonType.CODING)
                .orderIndex(1)
                .content("<h2>Read</h2><script>alert(1)</script><p onclick=\"bad()\"><a href=\"javascript:bad()\">Open</a></p>")
                .build();
        secondLesson = Lesson.builder()
                .id(102)
                .course(course)
                .title("Video Lesson")
                .type(LessonType.VIDEO)
                .orderIndex(2)
                .content("<p>Watch this.</p>")
                .video(Video.builder().videoUrl("/student/media/video-102.mp4").durationSeconds(90).build())
                .build();
    }

    @Test
    void enrolledStudentCanOpenOrderedCurriculumAndFirstLesson() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson, firstLesson));
        when(lessonProgressRepository.findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(20)).thenReturn(Optional.empty());
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 101)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(101, response.getActiveLessonId());
        assertEquals(List.of(101, 102), response.getLessons().stream().map(item -> item.getId()).toList());
        assertEquals(102, response.getActiveLesson().getNextLessonId());
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("<script>"));
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("onclick"));
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("javascript:"));
    }

    @Test
    void enrolledStudentCanViewVideoLessonAndLastAccessedIsUpdatedWithoutCompletion() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentLearningLessonDTO response = service.openLesson(10, 102);

        assertEquals("/student/media/video-102.mp4", response.getVideoUrl());
        assertEquals(101, response.getPreviousLessonId());
        assertNull(response.getNextLessonId());
        assertFalse(response.getCompleted());

        ArgumentCaptor<LessonProgress> progressCaptor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(lessonProgressRepository).save(progressCaptor.capture());
        assertEquals(false, progressCaptor.getValue().getIsCompleted());
        assertNotNull(progressCaptor.getValue().getLastAccessedAt());
    }

    @Test
    void studentWithoutEnrollmentIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 10)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getLearningCourse(10, null));
    }

    @Test
    void unavailableCourseIsRejected() {
        course.setStatus(CourseStatus.HIDDEN);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        assertThrows(BusinessException.class, () -> service.getLearningCourse(10, null));
        verify(enrollmentRepository, never()).findByStudentIdAndCourseIdForUpdate(any(), any());
    }

    @Test
    void changedLessonIdFromOtherCourseIsRejected() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson));

        assertThrows(BusinessException.class, () -> service.openLesson(10, 999));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void continueLearningUsesLastAccessedLessonWhenStillInCourse() {
        stubAccess();
        LessonProgress lastProgress = LessonProgress.builder()
                .id(33)
                .enrollment(enrollment)
                .lesson(secondLesson)
                .lastAccessedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(20)).thenReturn(Optional.of(lastProgress));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(lastProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(lastProgress));

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(102, response.getActiveLessonId());
        assertEquals(101, response.getActiveLesson().getPreviousLessonId());
    }

    @Test
    void higherVideoPositionUpdatesProgressAndLowerDelayedRequestDoesNotReduceIt() {
        stubAccess();
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(30)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(progress));

        LearningProgressDTO higher = service.recordVideoProgress(10, 102, 60);
        LearningProgressDTO lower = service.recordVideoProgress(10, 102, 20);

        assertEquals(60, higher.getWatchedSeconds());
        assertEquals(60, lower.getWatchedSeconds());
        assertFalse(lower.getCompleted());
    }

    @Test
    void arbitraryLargeVideoPositionIsCappedAndCannotInstantlyComplete() {
        stubAccess();
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(0)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(progress));

        LearningProgressDTO response = service.recordVideoProgress(10, 102, 90);

        assertEquals(45, response.getWatchedSeconds());
        assertFalse(response.getCompleted());
        assertNull(progress.getCompletedAt());
    }

    @Test
    void negativeVideoPositionIsRejected() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson));

        assertThrows(BusinessException.class, () -> service.recordVideoProgress(10, 102, -1));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void validContentLessonCompletionIsIdempotentAndUpdatesCourseProgress() {
        stubAccess();
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(firstLesson)
                .isCompleted(false)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 101)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(progress));

        LearningProgressDTO first = service.completeLesson(10, 101);
        LocalDateTime completedAt = progress.getCompletedAt();
        LearningProgressDTO repeated = service.completeLesson(10, 101);

        assertTrue(first.getCompleted());
        assertEquals(completedAt, progress.getCompletedAt());
        assertEquals(new BigDecimal("50.00"), repeated.getProgressPercentage());
        verify(notificationService, never()).createCourseCompletedNotification(any());
    }

    @Test
    void completingFinalRequiredLessonCreatesCourseCompletionNotification() {
        stubAccess();
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(firstLesson)
                .isCompleted(false)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 101)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(progress));

        LearningProgressDTO response = service.completeLesson(10, 101);

        assertTrue(response.getCompleted());
        assertEquals(new BigDecimal("100.00"), response.getProgressPercentage());
        verify(notificationService).createCourseCompletedNotification(enrollment);
        verify(certificateService).issueAutomaticallyIfEligible(enrollment.getId());
    }

    @Test
    void assessmentLessonCannotBeCompletedByGenericEndpoint() {
        stubAccess();
        Lesson assignmentLesson = Lesson.builder()
                .id(103)
                .course(course)
                .title("Assignment")
                .type(LessonType.CODING)
                .orderIndex(3)
                .codingassignment(CodingAssignment.builder().id(88).build())
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(assignmentLesson));

        assertThrows(BusinessException.class, () -> service.completeLesson(10, 103));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void courseProgressHandlesZeroRequiredLessons() {
        stubAccess();
        Lesson quizLesson = Lesson.builder()
                .id(104)
                .course(course)
                .title("Quiz")
                .type(LessonType.QUIZ)
                .orderIndex(1)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(quizLesson));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        LearningProgressDTO progress = service.getCourseProgress(10);

        assertEquals(0, progress.getTotalLessons());
        assertEquals(BigDecimal.ZERO, progress.getProgressPercentage());
        assertFalse(progress.getCompleted());
    }

    @Test
    void resumeFallsBackToFirstIncompleteWhenLastAccessedLessonIsUnavailable() {
        stubAccess();
        LessonProgress completedFirst = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(firstLesson)
                .isCompleted(true)
                .build();
        Lesson removedLesson = Lesson.builder().id(999).course(course).title("Removed").type(LessonType.VIDEO).orderIndex(9).build();
        LessonProgress removedProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(removedLesson)
                .lastAccessedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(20)).thenReturn(Optional.of(removedProgress));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(20, 101)).thenReturn(Optional.of(completedFirst));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(20, 102)).thenReturn(Optional.empty());
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst));

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(102, response.getActiveLessonId());
    }

    private void stubAccess() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 10)).thenReturn(Optional.of(enrollment));
    }
}
