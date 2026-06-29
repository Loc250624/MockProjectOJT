package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.mapper.CourseMapper;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Override
    public List<CourseResponseDTO> findAll() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CourseResponseDTO findById(Integer id) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return courseMapper.toDto(entity);
    }

    @Override
    public CourseResponseDTO create(CourseRequestDTO requestDTO) {
        Course entity = courseMapper.toEntity(requestDTO);
        Course saved = courseRepository.save(entity);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseResponseDTO update(Integer id, CourseRequestDTO requestDTO) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found");
        }
        Course entity = courseMapper.toEntity(requestDTO);
        entity.setId(id);
        Course updated = courseRepository.save(entity);
        return courseMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        courseRepository.deleteById(id);
    }
}
