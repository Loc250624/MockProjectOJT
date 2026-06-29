package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Blog;
import com.ojtsu26.elearning.dto.request.BlogRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogResponseDTO;
import com.ojtsu26.elearning.mapper.BlogMapper;
import com.ojtsu26.elearning.repository.BlogRepository;
import com.ojtsu26.elearning.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;

    @Override
    public List<BlogResponseDTO> findAll() {
        return blogRepository.findAll().stream()
                .map(blogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BlogResponseDTO findById(Integer id) {
        Blog entity = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        return blogMapper.toDto(entity);
    }

    @Override
    public BlogResponseDTO create(BlogRequestDTO requestDTO) {
        Blog entity = blogMapper.toEntity(requestDTO);
        Blog saved = blogRepository.save(entity);
        return blogMapper.toDto(saved);
    }

    @Override
    public BlogResponseDTO update(Integer id, BlogRequestDTO requestDTO) {
        if (!blogRepository.existsById(id)) {
            throw new RuntimeException("Blog not found");
        }
        Blog entity = blogMapper.toEntity(requestDTO);
        entity.setId(id);
        Blog updated = blogRepository.save(entity);
        return blogMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        blogRepository.deleteById(id);
    }
}
