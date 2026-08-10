package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CategoryResponseDTO {
    private Integer id;

    private String name;

    private String description;

    private long courseCount;
}
