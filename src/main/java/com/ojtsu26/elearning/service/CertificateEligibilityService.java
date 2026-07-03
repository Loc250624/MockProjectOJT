package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.CertificateEligibilityResponse;

public interface CertificateEligibilityService {
    CertificateEligibilityResponse evaluateEligibility(Integer enrollmentId, Integer currentStudentId);
}
