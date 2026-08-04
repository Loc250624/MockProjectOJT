package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentCourseProgressCardDTO;
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
                .type(null)
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
        assertFalse(response.getLessons().get(0).getLocked());
        assertTrue(response.getLessons().get(1).getLocked());
        assertEquals("Complete \"Text Lesson\" to unlock this lesson.", response.getLessons().get(1).getLockReason());
        assertFalse(response.getActiveLesson().getNextLessonAccessible());
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("<script>"));
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("onclick"));
        assertFalse(response.getActiveLesson().getSanitizedContent().contains("javascript:"));
    }

    @Test
    void secondLessonDirectAccessIsDeniedUntilPreviousRequiredLessonCompleted() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.openLesson(10, 102));

        assertEquals("Complete \"Text Lesson\" to unlock this lesson.", exception.getMessage());
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void lockedLessonApiProgressUpdateIsDeniedBeforeSaving() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> service.recordVideoProgress(10, 102, videoProgressRequest(10, 10, 90, "TIME_UPDATE")));

        verify(lessonProgressRepository, never()).findByEnrollmentIdAndLessonIdForUpdate(any(), any());
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void completingFirstRequiredLessonUnlocksNextRequiredLesson() {
        stubAccess();
        LessonProgress firstProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(firstLesson)
                .isCompleted(false)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 101)).thenReturn(Optional.of(firstProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(firstProgress));

        LearningProgressDTO response = service.completeLesson(10, 101);

        assertTrue(response.getCompleted());
        assertEquals(102, response.getNextLessonId());
        assertTrue(response.getNextLessonAccessible());
    }

    @Test
    void courseWideOrderingUsesOrderIndexInsteadOfLessonId() {
        stubAccess();
        Lesson orderOneHighId = Lesson.builder()
                .id(300)
                .course(course)
                .title("Order One")
                .type(null)
                .orderIndex(1)
                .build();
        Lesson orderTwoLowId = Lesson.builder()
                .id(100)
                .course(course)
                .title("Order Two")
                .type(null)
                .orderIndex(2)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(orderTwoLowId, orderOneHighId));
        when(lessonProgressRepository.findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(20)).thenReturn(Optional.empty());
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 300)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(300, response.getActiveLessonId());
        assertEquals(List.of(300, 100), response.getLessons().stream().map(item -> item.getId()).toList());
        assertTrue(response.getLessons().stream().filter(item -> item.getId().equals(100)).findFirst().orElseThrow().getLocked());
    }

    @Test
    void completionUnlocksNextRequiredLessonAcrossCourseWideOrderBoundaries() {
        stubAccess();
        Lesson thirdLesson = Lesson.builder()
                .id(103)
                .course(course)
                .title("Next Chapter First Lesson")
                .type(null)
                .orderIndex(3)
                .build();
        LessonProgress completedFirst = completedProgress(firstLesson);
        LessonProgress secondProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(0)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson, thirdLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(secondProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst, secondProgress));

        LearningProgressDTO response = service.recordVideoProgress(10, 102, videoProgressRequest(90, 90, 90, "ENDED"));

        assertTrue(response.getCompleted());
        assertEquals(103, response.getNextLessonId());
        assertTrue(response.getNextLessonAccessible());
    }

    @Test
    void videoCompletionUnlocksNextQuizLessonAfterVideo() {
        stubAccess();
        Lesson quizLesson = Lesson.builder()
                .id(103)
                .course(course)
                .title("Check It")
                .type(LessonType.QUIZ)
                .orderIndex(3)
                .build();
        LessonProgress completedFirst = completedProgress(firstLesson);
        LessonProgress videoProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(0)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson, quizLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(videoProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst, videoProgress));

        LearningProgressDTO response = service.recordVideoProgress(10, 102, videoProgressRequest(90, 90, 90, "ENDED"));

        assertTrue(response.getCompleted());
        assertEquals(103, response.getNextLessonId());
        assertTrue(response.getNextLessonAccessible());
    }

    @Test
    void videoCompletionUnlocksNextQuizLesson() {
        stubAccess();
        Lesson quizLesson = Lesson.builder()
                .id(104)
                .course(course)
                .title("Check Understanding")
                .type(LessonType.QUIZ)
                .orderIndex(3)
                .build();
        LessonProgress completedFirst = completedProgress(firstLesson);
        LessonProgress videoProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(0)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson, quizLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(videoProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst, videoProgress));

        LearningProgressDTO response = service.recordVideoProgress(10, 102, videoProgressRequest(90, 90, 90, "ENDED"));

        assertTrue(response.getCompleted());
        assertEquals(104, response.getNextLessonId());
        assertTrue(response.getNextLessonAccessible());
    }

    @Test
    void anotherStudentsCompletionDoesNotUnlockCurrentStudentsLesson() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.openLesson(10, 102));

        verify(lessonProgressRepository).findByEnrollmentId(20);
        verify(lessonProgressRepository, never()).findByEnrollmentId(21);
    }

    @Test
    void enrolledStudentCanViewVideoLessonAndLastAccessedIsUpdatedWithoutCompletion() {
        stubAccess();
        LessonProgress completedFirst = completedProgress(firstLesson);
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst));

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
    void enrolledStudentCanViewYouTubeLessonAndEmbedUrlIsMapped() {
        stubAccess();
        LessonProgress completedFirst = completedProgress(firstLesson);
        Lesson youtubeLesson = Lesson.builder()
                .id(103)
                .course(course)
                .title("YouTube Lesson")
                .type(LessonType.VIDEO)
                .orderIndex(3)
                .content("<p>Watch this on YouTube.</p>")
                .video(Video.builder().videoUrl("https://www.youtube.com/watch?v=qz0aGYrrIhU").durationSeconds(90).build())
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, youtubeLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 103)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst));

        StudentLearningLessonDTO response = service.openLesson(10, 103);

        assertEquals("https://www.youtube.com/watch?v=qz0aGYrrIhU", response.getVideoUrl());
        assertEquals("https://www.youtube.com/embed/qz0aGYrrIhU?enablejsapi=1", response.getEmbedUrl());
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
        LessonProgress completedFirst = completedProgress(firstLesson);
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
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst, lastProgress));

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(102, response.getActiveLessonId());
        assertEquals(101, response.getActiveLesson().getPreviousLessonId());
    }

    @Test
    void higherVideoPositionUpdatesProgressAndLowerDelayedRequestDoesNotReduceIt() {
        stubAccess();
        LessonProgress completedFirst = completedProgress(firstLesson);
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(30)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(firstLesson, secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst, progress));

        LearningProgressDTO higher = service.recordVideoProgress(10, 102, videoProgressRequest(60, 60, 90, "TIME_UPDATE"));
        LearningProgressDTO lower = service.recordVideoProgress(10, 102, videoProgressRequest(20, 20, 90, "TIME_UPDATE"));

        assertEquals(60, higher.getWatchedSeconds());
        assertEquals(60, lower.getWatchedSeconds());
        assertFalse(lower.getCompleted());
    }

    @Test
    void seekingForwardIsAcceptedAndCanCompleteAtThreshold() {
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

        LearningProgressDTO response = service.recordVideoProgress(10, 102, videoProgressRequest(81, 81, 90, "SEEKED"));

        assertEquals(81, response.getWatchedSeconds());
        assertEquals(81, response.getMaxReachedSeconds());
        assertEquals(81, response.getLastPositionSeconds());
        assertTrue(response.getCompleted());
        assertNotNull(progress.getCompletedAt());
    }

    @Test
    void negativeVideoPositionIsRejected() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson));

        assertThrows(BusinessException.class, () -> service.recordVideoProgress(10, 102, videoProgressRequest(-1, -1, 90, "TIME_UPDATE")));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void videoPositionAboveDurationIsRejected() {
        stubAccess();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson));

        assertThrows(BusinessException.class, () -> service.recordVideoProgress(10, 102, videoProgressRequest(91, 91, 90, "SEEKED")));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void endedEventCompletesVideoIdempotentlyWithoutDuplicatingCourseCompletionSideEffects() {
        stubAccess();
        enrollment.setIsCompleted(false);
        LessonProgress progress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(secondLesson)
                .isCompleted(false)
                .watchedSeconds(10)
                .maxReachedSeconds(10)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(secondLesson));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(progress));

        LearningProgressDTO first = service.recordVideoProgress(10, 102, videoProgressRequest(90, 90, 90, "ENDED"));
        LocalDateTime completedAt = progress.getCompletedAt();
        LearningProgressDTO repeated = service.recordVideoProgress(10, 102, videoProgressRequest(90, 90, 90, "ENDED"));

        assertTrue(first.getCompleted());
        assertTrue(repeated.getCourseCompleted());
        assertEquals("COMPLETED", repeated.getCourseStatus());
        assertEquals(completedAt, progress.getCompletedAt());
        verify(notificationService).createCourseCompletedNotification(enrollment);
        verify(certificateService).issueAutomaticallyIfEligible(enrollment.getId());
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
        Lesson quizLesson = Lesson.builder()
                .id(103)
                .course(course)
                .title("Quiz")
                .type(LessonType.QUIZ)
                .orderIndex(3)
                .build();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(10)).thenReturn(List.of(quizLesson));

        assertThrows(BusinessException.class, () -> service.completeLesson(10, 103));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void courseProgressIncludesQuizLessonsInRequiredCurriculum() {
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

        assertEquals(1, progress.getTotalLessons());
        assertEquals(new BigDecimal("0.00"), progress.getProgressPercentage());
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
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 102)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonProgressRepository.findByEnrollmentId(20)).thenReturn(List.of(completedFirst));

        StudentLearningCourseDTO response = service.getLearningCourse(10, null);

        assertEquals(102, response.getActiveLessonId());
    }

    @Test
    void courseCardsPutMostRecentlyEnrolledFirstRegardlessOfCompletionOrProgress() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 8, 0);
        Course completedCourse = cardCourse(11, "Completed");
        Course highProgressCourse = cardCourse(12, "Seventy five");
        Course earlierTieCourse = cardCourse(13, "Fifty earlier");
        Course laterTieCourse = cardCourse(14, "Fifty later");
        Course notStartedCourse = cardCourse(15, "Not started");

        CourseEnrollment completedEnrollment = cardEnrollment(21, completedCourse, true, base);
        CourseEnrollment highProgressEnrollment = cardEnrollment(22, highProgressCourse, false, base.plusDays(1));
        CourseEnrollment earlierTieEnrollment = cardEnrollment(23, earlierTieCourse, false, base.plusDays(2));
        CourseEnrollment laterTieEnrollment = cardEnrollment(24, laterTieCourse, false, base.plusDays(3));
        CourseEnrollment notStartedEnrollment = cardEnrollment(25, notStartedCourse, false, base.plusDays(4));

        when(enrollmentRepository.findByStudentId(1)).thenReturn(List.of(
                notStartedEnrollment,
                laterTieEnrollment,
                completedEnrollment,
                earlierTieEnrollment,
                highProgressEnrollment));

        stubCardProgress(completedEnrollment, 0, 0);
        stubCardProgress(highProgressEnrollment, 4, 3);
        stubCardProgress(earlierTieEnrollment, 2, 1);
        stubCardProgress(laterTieEnrollment, 2, 1);
        stubCardProgress(notStartedEnrollment, 0, 0);

        List<StudentCourseProgressCardDTO> cards = service.getCurrentStudentCourseCards();

        assertEquals(
                List.of("Not started", "Fifty later", "Fifty earlier", "Seventy five", "Completed"),
                cards.stream().map(StudentCourseProgressCardDTO::getCourseTitle).toList());
        assertEquals(new BigDecimal("0"), cards.get(0).getProgressPercentage());
        assertEquals(new BigDecimal("100.00"), cards.get(4).getProgressPercentage());
    }

    private void stubAccess() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 10)).thenReturn(Optional.of(enrollment));
    }

    private Course cardCourse(Integer id, String title) {
        return Course.builder()
                .id(id)
                .title(title)
                .status(CourseStatus.APPROVED)
                .build();
    }

    private CourseEnrollment cardEnrollment(Integer id, Course cardCourse, boolean completed,
                                              LocalDateTime enrolledAt) {
        return CourseEnrollment.builder()
                .id(id)
                .student(student)
                .course(cardCourse)
                .isCompleted(completed)
                .progressPercentage(BigDecimal.ZERO)
                .enrolledAt(enrolledAt)
                .build();
    }

    private void stubCardProgress(CourseEnrollment cardEnrollment, int totalLessons, int completedLessons) {
        List<Lesson> lessons = java.util.stream.IntStream.range(0, totalLessons)
                .mapToObj(index -> Lesson.builder()
                        .id(cardEnrollment.getCourse().getId() * 100 + index)
                        .course(cardEnrollment.getCourse())
                        .title("Lesson " + index)
                        .type(LessonType.VIDEO)
                        .orderIndex(index)
                        .build())
                .toList();
        List<LessonProgress> progresses = java.util.stream.IntStream.range(0, completedLessons)
                .mapToObj(index -> LessonProgress.builder()
                        .enrollment(cardEnrollment)
                        .lesson(lessons.get(index))
                        .isCompleted(true)
                        .build())
                .toList();
        when(lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(cardEnrollment.getCourse().getId()))
                .thenReturn(lessons);
        when(lessonProgressRepository.findByEnrollmentId(cardEnrollment.getId())).thenReturn(progresses);
    }

    private LessonProgress completedProgress(Lesson lesson) {
        return LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(lesson)
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .build();
    }

    private VideoProgressRequestDTO videoProgressRequest(Integer current, Integer maxReached, Integer duration, String eventType) {
        VideoProgressRequestDTO request = new VideoProgressRequestDTO();
        request.setCurrentTimeSeconds(current);
        request.setMaxReachedSeconds(maxReached);
        request.setWatchedSeconds(maxReached);
        request.setDurationSeconds(duration);
        request.setEventType(eventType);
        return request;
    }
}
