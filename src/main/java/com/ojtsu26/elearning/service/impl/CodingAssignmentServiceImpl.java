package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.dto.request.CodingAssignmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CodingAssignmentResponseDTO;
import com.ojtsu26.elearning.mapper.CodingAssignmentMapper;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.service.CodingAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodingAssignmentServiceImpl implements CodingAssignmentService {

    private final CodingAssignmentRepository codingAssignmentRepository;
    private final CodingAssignmentMapper codingAssignmentMapper;

    @Override
    public List<CodingAssignmentResponseDTO> findAll() {
        return codingAssignmentRepository.findAll().stream()
                .map(codingAssignmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CodingAssignmentResponseDTO findById(Integer id) {
        CodingAssignment entity = codingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CodingAssignment not found"));
        return codingAssignmentMapper.toDto(entity);
    }

    @Override
    public CodingAssignmentResponseDTO create(CodingAssignmentRequestDTO requestDTO) {
        CodingAssignment entity = codingAssignmentMapper.toEntity(requestDTO);
        CodingAssignment saved = codingAssignmentRepository.save(entity);
        return codingAssignmentMapper.toDto(saved);
    }

    @Override
    public CodingAssignmentResponseDTO update(Integer id, CodingAssignmentRequestDTO requestDTO) {
        if (!codingAssignmentRepository.existsById(id)) {
            throw new RuntimeException("CodingAssignment not found");
        }
        CodingAssignment entity = codingAssignmentMapper.toEntity(requestDTO);
        entity.setId(id);
        CodingAssignment updated = codingAssignmentRepository.save(entity);
        return codingAssignmentMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        codingAssignmentRepository.deleteById(id);
    }
}
