package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.CertificateStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CertificateResponseDTO {
    private Integer id;
    private Integer enrollmentId;
    private Integer courseId;
    private String studentName;
    private String courseName;
    private String teacherName;
    private LocalDateTime issuedAt;
    private String verificationCode;
    private CertificateStatus status;
    private String downloadUrl;
    private String verifyUrl;
}
