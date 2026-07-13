package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentCodeExampleDTO {
    private Integer id;
    private String inputData;
}
