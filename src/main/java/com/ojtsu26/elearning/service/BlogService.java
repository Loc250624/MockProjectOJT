package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.BlogRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogResponseDTO;
import java.util.List;

public interface BlogService {
    List<BlogResponseDTO> findAll();
    BlogResponseDTO findById(Integer id);
    BlogResponseDTO create(BlogRequestDTO requestDTO);
    BlogResponseDTO update(Integer id, BlogRequestDTO requestDTO);
    void delete(Integer id);
}
