package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.QuestionRequestDTO;
import com.ojtsu26.elearning.dto.response.QuestionResponseDTO;
import java.util.List;

public interface QuestionService {
    List<QuestionResponseDTO> findAll();
    QuestionResponseDTO findById(Integer id);
    QuestionResponseDTO create(QuestionRequestDTO requestDTO);
    QuestionResponseDTO update(Integer id, QuestionRequestDTO requestDTO);
    void delete(Integer id);
}
