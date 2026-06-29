package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.mapper.LessonMapper;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;

    @Override
    public List<LessonResponseDTO> findAll() {
        return lessonRepository.findAll().stream()
                .map(lessonMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LessonResponseDTO findById(Integer id) {
        Lesson entity = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return lessonMapper.toDto(entity);
    }

    @Override
    public LessonResponseDTO create(LessonRequestDTO requestDTO) {
        Lesson entity = lessonMapper.toEntity(requestDTO);
        Lesson saved = lessonRepository.save(entity);
        return lessonMapper.toDto(saved);
    }

    @Override
    public LessonResponseDTO update(Integer id, LessonRequestDTO requestDTO) {
        if (!lessonRepository.existsById(id)) {
            throw new RuntimeException("Lesson not found");
        }
        Lesson entity = lessonMapper.toEntity(requestDTO);
        entity.setId(id);
        Lesson updated = lessonRepository.save(entity);
        return lessonMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        lessonRepository.deleteById(id);
    }
}
