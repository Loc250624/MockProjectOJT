package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentCertificateDashboardDTO {
    private List<CertificateResponseDTO> certificates;
    private long totalCertificates;
}
