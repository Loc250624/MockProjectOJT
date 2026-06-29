package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import com.ojtsu26.elearning.mapper.CategoryMapper;
import com.ojtsu26.elearning.repository.CategoryRepository;
import com.ojtsu26.elearning.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponseDTO findById(Integer id) {
        Category entity = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryMapper.toDto(entity);
    }

    @Override
    public CategoryResponseDTO create(CategoryRequestDTO requestDTO) {
        Category entity = categoryMapper.toEntity(requestDTO);
        Category saved = categoryRepository.save(entity);
        return categoryMapper.toDto(saved);
    }

    @Override
    public CategoryResponseDTO update(Integer id, CategoryRequestDTO requestDTO) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }
        Category entity = categoryMapper.toEntity(requestDTO);
        entity.setId(id);
        Category updated = categoryRepository.save(entity);
        return categoryMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        categoryRepository.deleteById(id);
    }
}
