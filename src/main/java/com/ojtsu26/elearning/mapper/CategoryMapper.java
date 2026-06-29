package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {


    CategoryResponseDTO toDto(Category entity);


    Category toEntity(CategoryRequestDTO dto);
}
