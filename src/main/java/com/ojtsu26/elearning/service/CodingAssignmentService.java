package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CodingAssignmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CodingAssignmentResponseDTO;
import java.util.List;

public interface CodingAssignmentService {
    List<CodingAssignmentResponseDTO> findAll();
    CodingAssignmentResponseDTO findById(Integer id);
    CodingAssignmentResponseDTO create(CodingAssignmentRequestDTO requestDTO);
    CodingAssignmentResponseDTO update(Integer id, CodingAssignmentRequestDTO requestDTO);
    void delete(Integer id);
}
