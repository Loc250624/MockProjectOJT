package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import com.ojtsu26.elearning.mapper.RoadmapMapper;
import com.ojtsu26.elearning.repository.RoadmapRepository;
import com.ojtsu26.elearning.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapMapper roadmapMapper;

    @Override
    public List<RoadmapResponseDTO> findAll() {
        return roadmapRepository.findAll().stream()
                .map(roadmapMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoadmapResponseDTO findById(Integer id) {
        Roadmap entity = roadmapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));
        return roadmapMapper.toDto(entity);
    }

    @Override
    public RoadmapResponseDTO create(RoadmapRequestDTO requestDTO) {
        if (roadmapRepository.existsByTitle(requestDTO.getTitle())) {
            throw new IllegalArgumentException("Roadmap title already exists");
        }
        Roadmap entity = roadmapMapper.toEntity(requestDTO);
        Roadmap saved = roadmapRepository.save(entity);
        return roadmapMapper.toDto(saved);
    }

    @Override
    public RoadmapResponseDTO update(Integer id, RoadmapRequestDTO requestDTO) {
        Roadmap existing = roadmapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));
        
        if (roadmapRepository.existsByTitleAndIdNot(requestDTO.getTitle(), id)) {
            throw new IllegalArgumentException("Roadmap title already exists");
        }
        
        existing.setTitle(requestDTO.getTitle());
        existing.setDescription(requestDTO.getDescription());
        
        if (requestDTO.getInstructorId() != null) {
            com.ojtsu26.elearning.model.entity.User instructor = new com.ojtsu26.elearning.model.entity.User();
            instructor.setId(requestDTO.getInstructorId());
            existing.setInstructor(instructor);
        }

        Roadmap updated = roadmapRepository.save(existing);
        return roadmapMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        Roadmap roadmap = roadmapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));
        if (roadmap.getCourses() != null && !roadmap.getCourses().isEmpty()) {
            throw new RuntimeException("Cannot delete roadmap as it is associated with existing courses");
        }
        roadmapRepository.delete(roadmap);
    }
}
