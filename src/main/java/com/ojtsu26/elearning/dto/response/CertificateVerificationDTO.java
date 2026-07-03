package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.CertificateStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateVerificationDTO {
    private boolean valid;
    private String status;
    private String verificationCode;
    private String studentNameSnapshot;
    private String courseNameSnapshot;
    private String teacherNameSnapshot;
    private LocalDateTime issuedAt;
    private CertificateStatus certificateStatus;
}
