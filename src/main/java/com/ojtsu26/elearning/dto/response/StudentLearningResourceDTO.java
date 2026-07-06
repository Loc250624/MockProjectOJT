package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentLearningResourceDTO {
    private Integer id;
    private String name;
    private String type;
    private Long sizeBytes;
    private Boolean downloadable;
    private String viewUrl;
    private String downloadUrl;
}
