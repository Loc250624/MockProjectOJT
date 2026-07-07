package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.mapper.CourseMapper;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final NotificationService notificationService;

    // ----------------------------------------------------------------
    // Basic CRUD
    // ----------------------------------------------------------------

    @Override
    public List<CourseResponseDTO> findAll() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> findByInstructorId(Integer instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> findByStatus(CourseStatus status) {
        return courseRepository.findByStatus(status).stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> findApprovedCourses(Integer categoryId, String keyword, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // default newest
        if ("price-low".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price-high".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("title".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "title");
        }

        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return courseRepository.findApprovedCourses(categoryId, cleanKeyword, sort).stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CourseResponseDTO> findApprovedCourses(Integer categoryId, String keyword, String sortBy, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // default newest
        if ("price-low".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price-high".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("title".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "title");
        }

        Pageable resolvedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), sort);

        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return courseRepository.findApprovedCourses(categoryId, cleanKeyword, resolvedPageable)
                .map(courseMapper::toDto);
    }

    @Override
    public CourseResponseDTO findById(Integer id) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        return courseMapper.toDto(entity);
    }

    @Override
    public CourseResponseDTO create(CourseRequestDTO requestDTO) {
        Course entity = courseMapper.toEntity(requestDTO);
        entity.setStatus(CourseStatus.DRAFT);   // Always start as DRAFT
        entity.setRejectReason(null);
        Course saved = courseRepository.save(entity);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO update(Integer id, CourseRequestDTO requestDTO) {
        Course existing = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        // Only mutable fields — never overwrite id, createdAt, instructor, status, rejectReason
        existing.setTitle(requestDTO.getTitle());
        existing.setDescription(requestDTO.getDescription());
        existing.setThumbnailUrl(requestDTO.getThumbnailUrl());
        existing.setPrice(requestDTO.getPrice());

        if (requestDTO.getCategoryId() != null) {
            Category category = new Category();
            category.setId(requestDTO.getCategoryId());
            existing.setCategory(category);
        }

        if (requestDTO.getRoadmapId() != null) {
            Roadmap roadmap = new Roadmap();
            roadmap.setId(requestDTO.getRoadmapId());
            existing.setRoadmap(roadmap);
        }

        Course updated = courseRepository.save(existing);
        return courseMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private Course resolveCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
    }

    private void verifyOwnership(Course course, Integer instructorId) {
        if (course.getInstructor() == null || !course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Access Denied: You do not own this course.");
        }
    }

    /**
     * Validates that a course meets all requirements before submission.
     * Returns a list of human-readable error messages. Empty list means valid.
     *
     * Rules:
     *   - Title must be non-blank
     *   - Description must be non-blank
     *   - Category must be assigned
     *   - At least one lesson must exist
     *   - VIDEO lessons must have an attached Video
     *   - QUIZ lessons must have an attached Quiz
     *   - CODING lessons must have an attached CodingAssignment
     */
    private List<String> validateForSubmission(Course course) {
        List<String> errors = new ArrayList<>();

        if (course.getTitle() == null || course.getTitle().isBlank()) {
            errors.add("Course title is required.");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            errors.add("Course description is required.");
        }
        if (course.getCategory() == null) {
            errors.add("A category must be assigned to the course.");
        }

        List<Lesson> lessons = course.getLessons();
        if (lessons == null || lessons.isEmpty()) {
            errors.add("The course must have at least one lesson.");
        } else {
            for (Lesson lesson : lessons) {
                if (lesson.getTitle() == null || lesson.getTitle().isBlank()) {
                    errors.add("Lesson #" + lesson.getOrderIndex() + " is missing a title.");
                }
                if (lesson.getType() == null) {
                    errors.add("Lesson #" + lesson.getOrderIndex() + " (" + lesson.getTitle() + ") has no type set.");
                    continue;
                }
                if (lesson.getType() == LessonType.VIDEO && lesson.getVideo() == null) {
                    errors.add("Lesson #" + lesson.getOrderIndex() + " (" + lesson.getTitle() + ") is a VIDEO lesson but has no video attached.");
                }
                if (lesson.getType() == LessonType.QUIZ && lesson.getQuiz() == null) {
                    errors.add("Lesson #" + lesson.getOrderIndex() + " (" + lesson.getTitle() + ") is a QUIZ lesson but has no quiz attached.");
                }
                if (lesson.getType() == LessonType.CODING && lesson.getCodingassignment() == null) {
                    errors.add("Lesson #" + lesson.getOrderIndex() + " (" + lesson.getTitle() + ") is a CODING lesson but has no coding assignment attached.");
                }
            }
        }

        return errors;
    }

    // ----------------------------------------------------------------
    // CRS-08: Publishing workflow (Teacher-side)
    // ----------------------------------------------------------------

    @Override
    public CourseResponseDTO submitForReview(Integer courseId, Integer instructorId) {
        Course course = resolveCourse(courseId);
        verifyOwnership(course, instructorId);

        // State guard: only DRAFT can be submitted
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new RuntimeException(
                "Only DRAFT courses can be submitted for review. Current status: " + course.getStatus());
        }

        // Business validation
        List<String> errors = validateForSubmission(course);
        if (!errors.isEmpty()) {
            throw new RuntimeException("Submission failed:\n• " + String.join("\n• ", errors));
        }

        course.setStatus(CourseStatus.PENDING_APPROVAL);
        course.setRejectReason(null);   // Clear any previous rejection reason
        Course saved = courseRepository.save(course);
        notificationService.createCourseSubmittedForReviewNotification(saved);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO withdrawSubmission(Integer courseId, Integer instructorId) {
        Course course = resolveCourse(courseId);
        verifyOwnership(course, instructorId);

        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                "Only PENDING_APPROVAL courses can be withdrawn. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.DRAFT);
        return courseMapper.toDto(courseRepository.save(course));
    }

    // ----------------------------------------------------------------
    // CRS-09: Approval workflow (Admin-side)
    // ----------------------------------------------------------------

    @Override
    public CourseResponseDTO approveCourse(Integer courseId) {
        Course course = resolveCourse(courseId);

        if (course.getStatus() == CourseStatus.APPROVED) {
            throw new RuntimeException("This course is already APPROVED.");
        }
        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                "Only PENDING_APPROVAL courses can be approved. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.APPROVED);
        course.setRejectReason(null);
        Course saved = courseRepository.save(course);
        notificationService.createCourseModerationNotification(saved, NotificationType.COURSE_APPROVED);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO rejectCourse(Integer courseId, String reason) {
        Course course = resolveCourse(courseId);

        if (course.getStatus() == CourseStatus.DRAFT) {
            throw new RuntimeException("DRAFT courses cannot be rejected — they have not been submitted.");
        }
        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                "Only PENDING_APPROVAL courses can be rejected. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.DRAFT);
        course.setRejectReason(reason != null && !reason.isBlank() ? reason : "No reason provided.");
        Course saved = courseRepository.save(course);
        notificationService.createCourseModerationNotification(saved, NotificationType.COURSE_REJECTED);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO hideCourse(Integer courseId) {
        Course course = resolveCourse(courseId);

        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new RuntimeException(
                "Only APPROVED courses can be hidden. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.HIDDEN);
        Course saved = courseRepository.save(course);
        notificationService.createCourseModerationNotification(saved, NotificationType.COURSE_HIDDEN);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO unhideCourse(Integer courseId) {
        Course course = resolveCourse(courseId);

        if (course.getStatus() != CourseStatus.HIDDEN) {
            throw new RuntimeException(
                "Only HIDDEN courses can be unhidden. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.APPROVED);
        Course saved = courseRepository.save(course);
        notificationService.createCourseModerationNotification(saved, NotificationType.COURSE_UNHIDDEN);
        return courseMapper.toDto(saved);
    }
}
