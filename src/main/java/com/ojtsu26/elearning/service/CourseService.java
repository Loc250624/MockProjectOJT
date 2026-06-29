package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> findAll();
    CourseResponseDTO findById(Integer id);
    CourseResponseDTO create(CourseRequestDTO requestDTO);
    CourseResponseDTO update(Integer id, CourseRequestDTO requestDTO);
    void delete(Integer id);
}
