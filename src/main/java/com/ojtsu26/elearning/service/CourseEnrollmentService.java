package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import java.util.List;

public interface CourseEnrollmentService {
    List<CourseEnrollmentResponseDTO> findAll();
    CourseEnrollmentResponseDTO findById(Integer id);
    CourseEnrollmentResponseDTO create(CourseEnrollmentRequestDTO requestDTO);
    CourseEnrollmentResponseDTO update(Integer id, CourseEnrollmentRequestDTO requestDTO);
    void delete(Integer id);
}
