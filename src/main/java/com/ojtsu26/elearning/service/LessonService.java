package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import java.util.List;

public interface LessonService {
    List<LessonResponseDTO> findAll();
    LessonResponseDTO findById(Integer id);
    LessonResponseDTO create(LessonRequestDTO requestDTO);
    LessonResponseDTO update(Integer id, LessonRequestDTO requestDTO);
    void delete(Integer id);
}
