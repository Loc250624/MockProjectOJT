package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentFeedbackResponseDTO;
import org.springframework.data.domain.Page;

public interface StudentFeedbackService {
    StudentFeedbackResponseDTO createForCurrentStudent(StudentFeedbackRequestDTO request);

    Page<StudentFeedbackResponseDTO> getAllFeedbacksForAdmin(int page, int size);

    StudentFeedbackResponseDTO getFeedbackDetailForAdmin(Integer feedbackId);
}
