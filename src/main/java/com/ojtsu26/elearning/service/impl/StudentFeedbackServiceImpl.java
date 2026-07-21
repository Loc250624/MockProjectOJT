package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentFeedbackResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.StudentFeedback;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.StudentFeedbackRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentFeedbackServiceImpl implements StudentFeedbackService {

    private static final int MAX_PAGE_SIZE = 50;

    private final StudentFeedbackRepository feedbackRepository;
    private final CurrentUserService currentUserService;

    @Override
    public StudentFeedbackResponseDTO createForCurrentStudent(StudentFeedbackRequestDTO request) {
        User student = requireCurrentUserWithRole(Role.STUDENT);
        String subject = normalizeRequired(request == null ? null : request.getSubject(), "Subject is required.");
        String content = normalizeRequired(request == null ? null : request.getContent(), "Content is required.");
        FeedbackCategory category = requireCategory(request == null ? null : request.getCategory());
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

        return toDto(feedbackRepository.save(feedback));
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

    private FeedbackCategory requireCategory(FeedbackCategory category) {
        if (category == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Category is required.");
        }
        return category;
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
