package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.Transaction;
import java.util.List;

public interface CourseEnrollmentService {
    List<CourseEnrollmentResponseDTO> findAll();
    CourseEnrollmentResponseDTO findById(Integer id);
    CourseEnrollmentResponseDTO create(CourseEnrollmentRequestDTO requestDTO);
    CourseEnrollmentResponseDTO update(Integer id, CourseEnrollmentRequestDTO requestDTO);
    void delete(Integer id);
    CourseEnrollmentResponseDTO enrollCurrentStudentInFreeCourse(Integer courseId);
    CourseEnrollmentStateResponseDTO getCurrentStudentCourseState(Integer courseId);
    CourseEnrollmentResponseDTO activateEnrollmentAfterVerifiedPayment(Order order, Transaction transaction);
}
