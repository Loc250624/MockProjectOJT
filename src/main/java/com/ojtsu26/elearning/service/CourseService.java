package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> findAll();
    List<CourseResponseDTO> findByInstructorId(Integer instructorId);
    List<CourseResponseDTO> findByStatus(CourseStatus status);
    List<CourseResponseDTO> findApprovedCourses(Integer categoryId, String keyword, String sortBy);
    Page<CourseResponseDTO> findApprovedCourses(Integer categoryId, String keyword, String sortBy, Pageable pageable);
    CourseResponseDTO findById(Integer id);
    CourseResponseDTO create(CourseRequestDTO requestDTO);
    CourseResponseDTO update(Integer id, CourseRequestDTO requestDTO);
    void delete(Integer id);

    // ----------------------------------------------------------------
    // CRS-08: Publishing workflow (Teacher-side)
    // ----------------------------------------------------------------

    /**
     * Validate and transition a DRAFT course to PENDING_APPROVAL.
     * Clears any previous rejectReason.
     * Throws if the teacher does not own the course or validation fails.
     */
    CourseResponseDTO submitForReview(Integer courseId, Integer instructorId);

    /**
     * Withdraw a PENDING_APPROVAL course back to DRAFT.
     * Only the course owner can withdraw.
     */
    CourseResponseDTO withdrawSubmission(Integer courseId, Integer instructorId);

    // ----------------------------------------------------------------
    // CRS-09: Approval workflow (Admin-side)
    // ----------------------------------------------------------------

    /** Transition PENDING_APPROVAL → APPROVED. */
    CourseResponseDTO approveCourse(Integer courseId);

    /**
     * Transition PENDING_APPROVAL → DRAFT and record the rejection reason.
     * The reason is persisted so the teacher can read it.
     */
    CourseResponseDTO rejectCourse(Integer courseId, String reason);

    /** Transition APPROVED → HIDDEN. */
    CourseResponseDTO hideCourse(Integer courseId);

    /** Admin: unhide a HIDDEN course back to APPROVED. */
    CourseResponseDTO unhideCourse(Integer courseId);
}
