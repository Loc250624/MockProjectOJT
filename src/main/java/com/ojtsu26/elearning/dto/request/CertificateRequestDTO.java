package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CertificateRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String certificateUrl;

    @jakarta.validation.constraints.NotNull
    private Integer studentId;

    @jakarta.validation.constraints.NotNull
    private Integer courseId;
}
