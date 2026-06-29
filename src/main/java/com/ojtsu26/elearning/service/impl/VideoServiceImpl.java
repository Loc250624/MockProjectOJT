package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.mapper.VideoMapper;
import com.ojtsu26.elearning.repository.VideoRepository;
import com.ojtsu26.elearning.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    @Override
    public List<VideoResponseDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(videoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public VideoResponseDTO findById(Integer id) {
        Video entity = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return videoMapper.toDto(entity);
    }

    @Override
    public VideoResponseDTO create(VideoRequestDTO requestDTO) {
        Video entity = videoMapper.toEntity(requestDTO);
        Video saved = videoRepository.save(entity);
        return videoMapper.toDto(saved);
    }

    @Override
    public VideoResponseDTO update(Integer id, VideoRequestDTO requestDTO) {
        if (!videoRepository.existsById(id)) {
            throw new RuntimeException("Video not found");
        }
        Video entity = videoMapper.toEntity(requestDTO);
        entity.setId(id);
        Video updated = videoRepository.save(entity);
        return videoMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        videoRepository.deleteById(id);
    }
}
