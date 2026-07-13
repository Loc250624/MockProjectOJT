package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.StudentAssessmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import com.ojtsu26.elearning.service.UserService;
import com.ojtsu26.elearning.security.JwtCookieService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class StudentActionControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtCookieService jwtCookieService;

    @Mock
    private CourseEnrollmentService courseEnrollmentService;

    @Mock
    private StudentLearningService studentLearningService;

    @Mock
    private StudentAssessmentService studentAssessmentService;

    @Test
    void videoProgressPassesServerProgressPayloadToLearningService() {
        StudentActionController controller = new StudentActionController(
                userService,
                jwtCookieService,
                courseEnrollmentService,
                studentLearningService,
                studentAssessmentService
        );
        VideoProgressRequestDTO request = new VideoProgressRequestDTO();
        request.setCurrentTimeSeconds(42);
        request.setMaxReachedSeconds(60);
        request.setWatchedSeconds(60);
        request.setDurationSeconds(90);
        request.setEventType("SEEKED");
        LearningProgressDTO progress = LearningProgressDTO.builder()
                .courseId(10)
                .lessonId(102)
                .progressPercentage(new BigDecimal("50.00"))
                .completed(false)
                .lessonCompleted(false)
                .courseStatus("IN_PROGRESS")
                .lastPositionSeconds(42)
                .maxReachedSeconds(60)
                .build();
        when(studentLearningService.recordVideoProgress(10, 102, request)).thenReturn(progress);

        ResponseEntity<ApiResponse<LearningProgressDTO>> response = controller.videoProgress(10, 102, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(42, response.getBody().getData().getLastPositionSeconds());
        assertEquals("IN_PROGRESS", response.getBody().getData().getCourseStatus());
        ArgumentCaptor<VideoProgressRequestDTO> requestCaptor = ArgumentCaptor.forClass(VideoProgressRequestDTO.class);
        verify(studentLearningService).recordVideoProgress(eq(10), eq(102), requestCaptor.capture());
        assertEquals("SEEKED", requestCaptor.getValue().getEventType());
    }

    @Test
    void lockedLessonApiDenialPropagatesFromLearningService() {
        StudentActionController controller = new StudentActionController(
                userService,
                jwtCookieService,
                courseEnrollmentService,
                studentLearningService,
                studentAssessmentService
        );
        when(studentLearningService.openLesson(10, 102))
                .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "Complete \"Intro\" to unlock this lesson."));

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.lesson(10, 102));

        assertEquals("Complete \"Intro\" to unlock this lesson.", exception.getMessage());
        verify(studentLearningService).openLesson(10, 102);
    }

    @Test
    void submitQuizPassesCourseLessonScopedRequestToAssessmentService() {
        StudentActionController controller = new StudentActionController(
                userService,
                jwtCookieService,
                courseEnrollmentService,
                studentLearningService,
                studentAssessmentService
        );
        StudentQuizSubmissionRequestDTO request = new StudentQuizSubmissionRequestDTO();
        request.setAttemptId(501);
        StudentQuizAttemptDTO responseDto = StudentQuizAttemptDTO.builder()
                .courseId(10)
                .lessonId(201)
                .attemptId(501)
                .submitted(true)
                .build();
        when(studentAssessmentService.submitQuiz(10, 201, request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<StudentQuizAttemptDTO>> response = controller.submitQuiz(10, 201, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(501, response.getBody().getData().getAttemptId());
        verify(studentAssessmentService).submitQuiz(10, 201, request);
    }
}
