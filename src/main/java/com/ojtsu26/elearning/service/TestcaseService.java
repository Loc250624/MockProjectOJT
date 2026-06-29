package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.TestcaseRequestDTO;
import com.ojtsu26.elearning.dto.response.TestcaseResponseDTO;
import java.util.List;

public interface TestcaseService {
    List<TestcaseResponseDTO> findAll();
    TestcaseResponseDTO findById(Integer id);
    TestcaseResponseDTO create(TestcaseRequestDTO requestDTO);
    TestcaseResponseDTO update(Integer id, TestcaseRequestDTO requestDTO);
    void delete(Integer id);
}
