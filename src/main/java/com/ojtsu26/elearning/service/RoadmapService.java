package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import java.util.List;

public interface RoadmapService {
    List<RoadmapResponseDTO> findAll();
    RoadmapResponseDTO findById(Integer id);
    RoadmapResponseDTO create(RoadmapRequestDTO requestDTO);
    RoadmapResponseDTO update(Integer id, RoadmapRequestDTO requestDTO);
    void delete(Integer id);
}
