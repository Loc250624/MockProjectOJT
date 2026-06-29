package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import java.util.List;

public interface CategoryService {
    List<CategoryResponseDTO> findAll();
    CategoryResponseDTO findById(Integer id);
    CategoryResponseDTO create(CategoryRequestDTO requestDTO);
    CategoryResponseDTO update(Integer id, CategoryRequestDTO requestDTO);
    void delete(Integer id);
}
