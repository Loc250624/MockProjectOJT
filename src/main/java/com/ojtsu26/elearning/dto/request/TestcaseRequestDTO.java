package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class TestcaseRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String inputData;

    @jakarta.validation.constraints.NotBlank
    private String expectedOutput;

    @jakarta.validation.constraints.NotNull
    private Boolean isHidden;

    @jakarta.validation.constraints.NotNull
    private Integer assignmentId;
}
