package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import com.ojtsu26.elearning.mapper.RoadmapMapper;
import com.ojtsu26.elearning.repository.RoadmapRepository;
import com.ojtsu26.elearning.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
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
        Roadmap entity = roadmapMapper.toEntity(requestDTO);
        Roadmap saved = roadmapRepository.save(entity);
        return roadmapMapper.toDto(saved);
    }

    @Override
    public RoadmapResponseDTO update(Integer id, RoadmapRequestDTO requestDTO) {
        if (!roadmapRepository.existsById(id)) {
            throw new RuntimeException("Roadmap not found");
        }
        Roadmap entity = roadmapMapper.toEntity(requestDTO);
        entity.setId(id);
        Roadmap updated = roadmapRepository.save(entity);
        return roadmapMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        roadmapRepository.deleteById(id);
    }
}
