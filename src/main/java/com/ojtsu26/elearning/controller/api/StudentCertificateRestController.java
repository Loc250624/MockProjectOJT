package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/certificates")
@RequiredArgsConstructor
public class StudentCertificateRestController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CertificateResponseDTO>>> list(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "12") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50),
                Sort.by(Sort.Direction.DESC, "issuedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok(ApiResponse.success(
                certificateService.getMyCertificates(userDetails.getUser().getId(), pageRequest)));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateResponseDTO>> detail(@PathVariable Integer certificateId,
                                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                certificateService.getMyCertificate(certificateId, userDetails.getUser().getId())));
    }

    @PostMapping("/claim/{enrollmentId}")
    public ResponseEntity<ApiResponse<CertificateResponseDTO>> claim(@PathVariable Integer enrollmentId,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                certificateService.issueIfEligible(enrollmentId, userDetails.getUser().getId()),
                "Certificate is ready"));
    }

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Integer certificateId,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        byte[] pdf = certificateService.generateCertificatePdf(certificateId, userDetails.getUser().getId());
        String filename = "lumina-certificate-" + certificateId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(new ByteArrayResource(pdf));
    }
}
