package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryDependencyResponseDTO {
    Integer categoryId;
    String categoryName;
    long dependentCourseCount;
    String message;
}
