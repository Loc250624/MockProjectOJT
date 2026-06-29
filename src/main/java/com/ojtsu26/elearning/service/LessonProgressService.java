package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.LessonProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonProgressResponseDTO;
import java.util.List;

public interface LessonProgressService {
    List<LessonProgressResponseDTO> findAll();
    LessonProgressResponseDTO findById(Integer id);
    LessonProgressResponseDTO create(LessonProgressRequestDTO requestDTO);
    LessonProgressResponseDTO update(Integer id, LessonProgressRequestDTO requestDTO);
    void delete(Integer id);
}
