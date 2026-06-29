package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.QuizRequestDTO;
import com.ojtsu26.elearning.dto.response.QuizResponseDTO;
import java.util.List;

public interface QuizService {
    List<QuizResponseDTO> findAll();
    QuizResponseDTO findById(Integer id);
    QuizResponseDTO create(QuizRequestDTO requestDTO);
    QuizResponseDTO update(Integer id, QuizRequestDTO requestDTO);
    void delete(Integer id);
}
