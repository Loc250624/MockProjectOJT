package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.CertificateVerificationDTO;
import com.ojtsu26.elearning.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/certificates")
@RequiredArgsConstructor
public class PublicCertificateRestController {

    private final CertificateService certificateService;

    @GetMapping("/verify/{verificationCode}")
    public ResponseEntity<ApiResponse<CertificateVerificationDTO>> verify(@PathVariable String verificationCode) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.verifyByCode(verificationCode)));
    }
}
