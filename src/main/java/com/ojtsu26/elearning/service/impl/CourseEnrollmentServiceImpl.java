package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.mapper.CourseEnrollmentMapper;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentServiceImpl implements CourseEnrollmentService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseEnrollmentMapper courseEnrollmentMapper;

    @Override
    public List<CourseEnrollmentResponseDTO> findAll() {
        return courseEnrollmentRepository.findAll().stream()
                .map(courseEnrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CourseEnrollmentResponseDTO findById(Integer id) {
        CourseEnrollment entity = courseEnrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CourseEnrollment not found"));
        return courseEnrollmentMapper.toDto(entity);
    }

    @Override
    public CourseEnrollmentResponseDTO create(CourseEnrollmentRequestDTO requestDTO) {
        CourseEnrollment entity = courseEnrollmentMapper.toEntity(requestDTO);
        CourseEnrollment saved = courseEnrollmentRepository.save(entity);
        return courseEnrollmentMapper.toDto(saved);
    }

    @Override
    public CourseEnrollmentResponseDTO update(Integer id, CourseEnrollmentRequestDTO requestDTO) {
        if (!courseEnrollmentRepository.existsById(id)) {
            throw new RuntimeException("CourseEnrollment not found");
        }
        CourseEnrollment entity = courseEnrollmentMapper.toEntity(requestDTO);
        entity.setId(id);
        CourseEnrollment updated = courseEnrollmentRepository.save(entity);
        return courseEnrollmentMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        courseEnrollmentRepository.deleteById(id);
    }
}
