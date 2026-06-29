package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.dto.request.LessonProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonProgressResponseDTO;
import com.ojtsu26.elearning.mapper.LessonProgressMapper;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonProgressMapper lessonProgressMapper;

    @Override
    public List<LessonProgressResponseDTO> findAll() {
        return lessonProgressRepository.findAll().stream()
                .map(lessonProgressMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LessonProgressResponseDTO findById(Integer id) {
        LessonProgress entity = lessonProgressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LessonProgress not found"));
        return lessonProgressMapper.toDto(entity);
    }

    @Override
    public LessonProgressResponseDTO create(LessonProgressRequestDTO requestDTO) {
        LessonProgress entity = lessonProgressMapper.toEntity(requestDTO);
        LessonProgress saved = lessonProgressRepository.save(entity);
        return lessonProgressMapper.toDto(saved);
    }

    @Override
    public LessonProgressResponseDTO update(Integer id, LessonProgressRequestDTO requestDTO) {
        if (!lessonProgressRepository.existsById(id)) {
            throw new RuntimeException("LessonProgress not found");
        }
        LessonProgress entity = lessonProgressMapper.toEntity(requestDTO);
        entity.setId(id);
        LessonProgress updated = lessonProgressRepository.save(entity);
        return lessonProgressMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        lessonProgressRepository.deleteById(id);
    }
}
