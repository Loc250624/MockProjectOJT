package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class TestcaseResponseDTO {
    private Integer id;

    private String inputData;

    private String expectedOutput;

    private Boolean isHidden;

    private Integer assignmentId;
}
