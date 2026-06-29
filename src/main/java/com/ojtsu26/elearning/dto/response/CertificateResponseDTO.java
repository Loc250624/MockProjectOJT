package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CertificateResponseDTO {
    private Integer id;

    private java.time.LocalDateTime issueDate;

    private String certificateUrl;

    private Integer studentId;

    private Integer courseId;
}
