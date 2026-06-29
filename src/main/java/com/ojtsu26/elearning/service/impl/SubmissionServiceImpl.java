package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.dto.request.SubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.SubmissionResponseDTO;
import com.ojtsu26.elearning.mapper.SubmissionMapper;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;

    @Override
    public List<SubmissionResponseDTO> findAll() {
        return submissionRepository.findAll().stream()
                .map(submissionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SubmissionResponseDTO findById(Integer id) {
        Submission entity = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        return submissionMapper.toDto(entity);
    }

    @Override
    public SubmissionResponseDTO create(SubmissionRequestDTO requestDTO) {
        Submission entity = submissionMapper.toEntity(requestDTO);
        Submission saved = submissionRepository.save(entity);
        return submissionMapper.toDto(saved);
    }

    @Override
    public SubmissionResponseDTO update(Integer id, SubmissionRequestDTO requestDTO) {
        if (!submissionRepository.existsById(id)) {
            throw new RuntimeException("Submission not found");
        }
        Submission entity = submissionMapper.toEntity(requestDTO);
        entity.setId(id);
        Submission updated = submissionRepository.save(entity);
        return submissionMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        submissionRepository.deleteById(id);
    }
}
