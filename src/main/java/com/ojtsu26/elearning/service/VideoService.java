package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import java.util.List;

public interface VideoService {
    List<VideoResponseDTO> findAll();
    VideoResponseDTO findById(Integer id);
    VideoResponseDTO create(VideoRequestDTO requestDTO);
    VideoResponseDTO update(Integer id, VideoRequestDTO requestDTO);
    void delete(Integer id);
}
