package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentFeedbackResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.feedback.email.FeedbackSubmittedEvent;
import com.ojtsu26.elearning.model.entity.StudentFeedback;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.StudentFeedbackRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentFeedbackServiceImpl implements StudentFeedbackService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final FeedbackCategory DEFAULT_FEEDBACK_CATEGORY = FeedbackCategory.OTHER;

    private final StudentFeedbackRepository feedbackRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public StudentFeedbackResponseDTO createForCurrentStudent(StudentFeedbackRequestDTO request) {
        User student = requireCurrentUserWithRole(Role.STUDENT);
        String subject = normalizeRequired(request == null ? null : request.getSubject(), "Subject is required.");
        String content = normalizeRequired(request == null ? null : request.getContent(), "Content is required.");
        FeedbackCategory category = categoryOrDefault(request == null ? null : request.getCategory());
        Integer courseContentRating = requireRating(request == null ? null : request.getCourseContentRating(), "Course content rating");
        Integer instructorSupportRating = requireRating(request == null ? null : request.getInstructorSupportRating(), "Instructor support rating");
        Integer learningExperienceRating = requireRating(request == null ? null : request.getLearningExperienceRating(), "Learning experience rating");
        Integer platformUsabilityRating = requireRating(request == null ? null : request.getPlatformUsabilityRating(), "Platform usability rating");
        Integer assessmentExperienceRating = requireRating(request == null ? null : request.getAssessmentExperienceRating(), "Assessment experience rating");
        Integer overallSatisfactionRating = requireRating(request == null ? null : request.getOverallSatisfactionRating(), "Overall satisfaction rating");

        StudentFeedback feedback = StudentFeedback.builder()
                .category(category)
                .subject(subject)
                .content(content)
                .courseContentRating(courseContentRating)
                .instructorSupportRating(instructorSupportRating)
                .learningExperienceRating(learningExperienceRating)
                .platformUsabilityRating(platformUsabilityRating)
                .assessmentExperienceRating(assessmentExperienceRating)
                .overallSatisfactionRating(overallSatisfactionRating)
                .student(student)
                .build();

        StudentFeedback saved = feedbackRepository.save(feedback);
        publishFeedbackSubmitted(saved, student);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentFeedbackResponseDTO> getAllFeedbacksForAdmin(int page, int size) {
        requireCurrentUserWithRole(Role.ADMIN);
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size))
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeedbackResponseDTO getFeedbackDetailForAdmin(Integer feedbackId) {
        requireCurrentUserWithRole(Role.ADMIN);
        return feedbackRepository.findWithStudentById(feedbackId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Feedback not found."));
    }

    private User requireCurrentUserWithRole(Role role) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != role) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return user;
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page index must not be negative.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        return PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return normalized;
    }

    private FeedbackCategory categoryOrDefault(FeedbackCategory category) {
        // Category is no longer user-selectable, but remains required by the
        // persisted/admin contract. Preserve explicit legacy callers and use
        // the existing general-purpose enum value for the simplified form.
        return category == null ? DEFAULT_FEEDBACK_CATEGORY : category;
    }

    private Integer requireRating(Integer rating, String label) {
        if (rating == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + " is required.");
        }
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + " must be between 1 and 5.");
        }
        return rating;
    }

    private void publishFeedbackSubmitted(StudentFeedback feedback, User student) {
        eventPublisher.publishEvent(new FeedbackSubmittedEvent(
                feedback.getId(),
                student.getFullName(),
                student.getEmail(),
                feedback.getSubject(),
                feedback.getContent(),
                feedback.getCourseContentRating(),
                feedback.getInstructorSupportRating(),
                feedback.getLearningExperienceRating(),
                feedback.getPlatformUsabilityRating(),
                feedback.getAssessmentExperienceRating(),
                feedback.getOverallSatisfactionRating(),
                feedback.getCreatedAt() == null ? LocalDateTime.now() : feedback.getCreatedAt()
        ));
    }

    private StudentFeedbackResponseDTO toDto(StudentFeedback feedback) {
        User student = feedback.getStudent();
        FeedbackCategory category = feedback.getCategory();
        return StudentFeedbackResponseDTO.builder()
                .id(feedback.getId())
                .category(category)
                .categoryLabel(category == null ? null : category.getDisplayName())
                .subject(feedback.getSubject())
                .content(feedback.getContent())
                .courseContentRating(feedback.getCourseContentRating())
                .instructorSupportRating(feedback.getInstructorSupportRating())
                .learningExperienceRating(feedback.getLearningExperienceRating())
                .platformUsabilityRating(feedback.getPlatformUsabilityRating())
                .assessmentExperienceRating(feedback.getAssessmentExperienceRating())
                .overallSatisfactionRating(feedback.getOverallSatisfactionRating())
                .studentId(student == null ? null : student.getId())
                .studentName(student == null ? null : student.getFullName())
                .studentEmail(student == null ? null : student.getEmail())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
