package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.dto.request.QuizRequestDTO;
import com.ojtsu26.elearning.dto.response.QuizResponseDTO;
import com.ojtsu26.elearning.mapper.QuizMapper;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;

    @Override
    public List<QuizResponseDTO> findAll() {
        return quizRepository.findAll().stream()
                .map(quizMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public QuizResponseDTO findById(Integer id) {
        Quiz entity = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        return quizMapper.toDto(entity);
    }

    @Override
    public QuizResponseDTO create(QuizRequestDTO requestDTO) {
        Quiz entity = quizMapper.toEntity(requestDTO);
        Quiz saved = quizRepository.save(entity);
        return quizMapper.toDto(saved);
    }

    @Override
    public QuizResponseDTO update(Integer id, QuizRequestDTO requestDTO) {
        if (!quizRepository.existsById(id)) {
            throw new RuntimeException("Quiz not found");
        }
        Quiz entity = quizMapper.toEntity(requestDTO);
        entity.setId(id);
        Quiz updated = quizRepository.save(entity);
        return quizMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        quizRepository.deleteById(id);
    }
}
