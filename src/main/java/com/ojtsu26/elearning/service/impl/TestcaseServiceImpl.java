package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.dto.request.TestcaseRequestDTO;
import com.ojtsu26.elearning.dto.response.TestcaseResponseDTO;
import com.ojtsu26.elearning.mapper.TestcaseMapper;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestcaseServiceImpl implements TestcaseService {

    private final TestcaseRepository testcaseRepository;
    private final TestcaseMapper testcaseMapper;

    @Override
    public List<TestcaseResponseDTO> findAll() {
        return testcaseRepository.findAll().stream()
                .map(testcaseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TestcaseResponseDTO findById(Integer id) {
        Testcase entity = testcaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testcase not found"));
        return testcaseMapper.toDto(entity);
    }

    @Override
    public TestcaseResponseDTO create(TestcaseRequestDTO requestDTO) {
        Testcase entity = testcaseMapper.toEntity(requestDTO);
        Testcase saved = testcaseRepository.save(entity);
        return testcaseMapper.toDto(saved);
    }

    @Override
    public TestcaseResponseDTO update(Integer id, TestcaseRequestDTO requestDTO) {
        if (!testcaseRepository.existsById(id)) {
            throw new RuntimeException("Testcase not found");
        }
        Testcase entity = testcaseMapper.toEntity(requestDTO);
        entity.setId(id);
        Testcase updated = testcaseRepository.save(entity);
        return testcaseMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        testcaseRepository.deleteById(id);
    }
}
