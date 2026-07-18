package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import java.util.List;

public interface LessonService {
    List<LessonResponseDTO> findAll();
    List<LessonResponseDTO> findByCourseId(Integer courseId, Integer instructorId);
    LessonResponseDTO findById(Integer id);
    LessonResponseDTO findById(Integer id, Integer instructorId);
    LessonResponseDTO create(LessonRequestDTO requestDTO, Integer instructorId);
    LessonResponseDTO update(Integer id, LessonRequestDTO requestDTO, Integer instructorId);
    void delete(Integer id, Integer instructorId);
    void reorderLessons(Integer courseId, List<Integer> lessonIdsInOrder, Integer instructorId);
}
