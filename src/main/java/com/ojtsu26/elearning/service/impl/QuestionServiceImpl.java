package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.dto.request.QuestionRequestDTO;
import com.ojtsu26.elearning.dto.response.QuestionResponseDTO;
import com.ojtsu26.elearning.mapper.QuestionMapper;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Override
    public List<QuestionResponseDTO> findAll() {
        return questionRepository.findAll().stream()
                .map(questionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public QuestionResponseDTO findById(Integer id) {
        Question entity = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return questionMapper.toDto(entity);
    }

    @Override
    public QuestionResponseDTO create(QuestionRequestDTO requestDTO) {
        Question entity = questionMapper.toEntity(requestDTO);
        Question saved = questionRepository.save(entity);
        return questionMapper.toDto(saved);
    }

    @Override
    public QuestionResponseDTO update(Integer id, QuestionRequestDTO requestDTO) {
        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Question not found");
        }
        Question entity = questionMapper.toEntity(requestDTO);
        entity.setId(id);
        Question updated = questionRepository.save(entity);
        return questionMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        questionRepository.deleteById(id);
    }
}
