package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.SubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.SubmissionResponseDTO;
import java.util.List;

public interface SubmissionService {
    List<SubmissionResponseDTO> findAll();
    SubmissionResponseDTO findById(Integer id);
    SubmissionResponseDTO create(SubmissionRequestDTO requestDTO);
    SubmissionResponseDTO update(Integer id, SubmissionRequestDTO requestDTO);
    void delete(Integer id);
}
