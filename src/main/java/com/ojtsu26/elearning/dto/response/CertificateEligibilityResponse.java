package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CertificateEligibilityResponse {
    private boolean eligible;
    private String status;
    private BigDecimal progressPercent;
    private long completedRequiredLessons;
    private long totalRequiredLessons;
    private long passedRequiredAssessments;
    private long totalRequiredAssessments;
    private List<String> unmetRequirements;
}
