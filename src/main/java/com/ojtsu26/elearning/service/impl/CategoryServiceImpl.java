package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryDependencyResponseDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.CategoryMapper;
import com.ojtsu26.elearning.repository.CategoryRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponseDTO findById(Integer id) {
        Category entity = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return toDto(entity);
    }

    @Override
    public CategoryResponseDTO create(CategoryRequestDTO requestDTO) {
        String name = normalizeName(requestDTO.getName());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
        Category entity = Category.builder()
                .name(name)
                .description(normalizeDescription(requestDTO.getDescription()))
                .build();
        Category saved = categoryRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public CategoryResponseDTO update(Integer id, CategoryRequestDTO requestDTO) {
        Category entity = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        String name = normalizeName(requestDTO.getName());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
        entity.setName(name);
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        return toDto(categoryRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        long courseCount = courseRepository.countByCategoryId(id);
        if (courseCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE, dependencyMessage(category, courseCount));
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDependencyResponseDTO dependencyInfo(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        long courseCount = courseRepository.countByCategoryId(id);
        return CategoryDependencyResponseDTO.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .dependentCourseCount(courseCount)
                .message(dependencyMessage(category, courseCount))
                .build();
    }

    @Override
    public CategoryDependencyResponseDTO reassignCoursesAndDelete(Integer id, Integer replacementCategoryId) {
        if (replacementCategoryId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Replacement category is required");
        }
        if (id.equals(replacementCategoryId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Replacement category must be different");
        }

        Category source = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        Category replacement = categoryRepository.findById(replacementCategoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Replacement category not found"));
        List<Course> courses = courseRepository.findByCategoryId(id);
        long movedCount = courses.size();
        courses.forEach(course -> course.setCategory(replacement));
        courseRepository.saveAll(courses);
        categoryRepository.delete(source);

        return CategoryDependencyResponseDTO.builder()
                .categoryId(source.getId())
                .categoryName(source.getName())
                .dependentCourseCount(movedCount)
                .message("Reassigned " + movedCount + " course(s) and deleted category.")
                .build();
    }

    private CategoryResponseDTO toDto(Category category) {
        CategoryResponseDTO dto = categoryMapper.toDto(category);
        dto.setCourseCount(courseRepository.countByCategoryId(category.getId()));
        return dto;
    }

    private String normalizeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Category name is required");
        }
        return value.trim();
    }

    private String normalizeDescription(String value) {
        return value == null ? "" : value.trim();
    }

    private String dependencyMessage(Category category, long courseCount) {
        return "Category '" + category.getName() + "' is assigned to " + courseCount
                + " course(s). Reassign the courses before deleting the category.";
    }
}
