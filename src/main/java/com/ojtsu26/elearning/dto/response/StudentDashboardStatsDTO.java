package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentDashboardStatsDTO {
    private long activeCertificates;
    private long revokedCertificates;
    private String certificateCaption;
    private String certificateCaptionClass;
}
