package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.CertificateEligibilityResponse;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.dto.response.CertificateVerificationDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.CertificateMapper;
import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.service.CertificateEligibilityService;
import com.ojtsu26.elearning.service.CertificateService;
import com.ojtsu26.elearning.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{20,64}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CertificateEligibilityService eligibilityService;
    private final CertificateMapper certificateMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CertificateResponseDTO issueIfEligible(Integer enrollmentId, Integer currentStudentId) {
        CourseEnrollment enrollment = enrollmentRepository.findByIdForCertificateIssue(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (enrollment.getStudent() == null || !enrollment.getStudent().getId().equals(currentStudentId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
        }

        return certificateRepository.findByEnrollmentId(enrollmentId)
                .map(certificateMapper::toDto)
                .orElseGet(() -> createCertificateAfterEligibility(enrollment, currentStudentId));
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public CertificateResponseDTO issueAutomaticallyIfEligible(Integer enrollmentId) {
        CourseEnrollment enrollment = enrollmentRepository.findByIdForCertificateIssue(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (enrollment.getStudent() == null) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
        }
        return certificateRepository.findByEnrollmentId(enrollmentId)
                .map(certificateMapper::toDto)
                .orElseGet(() -> createCertificateAfterEligibility(enrollment, enrollment.getStudent().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateResponseDTO> getMyCertificates(Integer currentStudentId, Pageable pageable) {
        return certificateRepository.findAllByStudentId(currentStudentId, pageable)
                .map(certificateMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponseDTO getMyCertificate(Integer certificateId, Integer currentStudentId) {
        Certificate certificate = findOwnedCertificate(certificateId, currentStudentId);
        return certificateMapper.toDto(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(Integer certificateId, Integer currentStudentId) {
        Certificate certificate = findOwnedCertificate(certificateId, currentStudentId);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDFont regular = loadUnicodeFont(document);
            PDFont bold = regular;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setLineWidth(2f);
                content.addRect(36, 36, 540, 720);
                content.stroke();
                writeCentered(content, bold, 18, 708, "LumiNa E-Learning Platform");
                writeCentered(content, bold, 30, 640, "Certificate of Completion");
                writeCentered(content, regular, 14, 585, "This certifies that");
                writeCentered(content, bold, 26, 540, safePdfText(certificate.getStudentNameSnapshot()));
                writeCentered(content, regular, 14, 500, "has successfully completed");
                writeCentered(content, bold, 22, 460, safePdfText(certificate.getCourseNameSnapshot()));
                writeCentered(content, regular, 13, 415, "Instructor: " + safePdfText(certificate.getTeacherNameSnapshot()));
                writeCentered(content, regular, 13, 385, "Issued: " + formatIssuedDate(certificate.getIssuedAt()));
                writeCentered(content, regular, 12, 330, "Verification code: " + certificate.getVerificationCode());
                writeCentered(content, regular, 11, 305,
                        "Verify at /certificates/verify/" + certificate.getVerificationCode());
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.CERTIFICATE_GENERATION_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateVerificationDTO verifyByCode(String verificationCode) {
        String normalized = normalizeVerificationCode(verificationCode);
        if (normalized == null) {
            return invalidVerification("INVALID_CODE", verificationCode);
        }
        return certificateRepository.findByVerificationCodeIgnoreCase(normalized)
                .map(this::toVerificationDto)
                .orElseGet(() -> invalidVerification("NOT_FOUND", normalized));
    }

    private CertificateResponseDTO createCertificateAfterEligibility(CourseEnrollment enrollment, Integer currentStudentId) {
        CertificateEligibilityResponse eligibility = eligibilityService.evaluateEligibility(enrollment.getId(), currentStudentId);
        if (!eligibility.isEligible()) {
            throw new BusinessException(ErrorCode.CERTIFICATE_NOT_ELIGIBLE,
                    "Certificate requirements are not met: " + String.join("; ", eligibility.getUnmetRequirements()));
        }

        Certificate certificate = buildCertificate(enrollment);
        try {
            Certificate saved = certificateRepository.saveAndFlush(certificate);
            createCertificateNotification(saved);
            return certificateMapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            return certificateRepository.findByEnrollmentId(enrollment.getId())
                    .map(certificateMapper::toDto)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATE_GENERATION_FAILED));
        }
    }

    private Certificate buildCertificate(CourseEnrollment enrollment) {
        User student = enrollment.getStudent();
        Course course = enrollment.getCourse();
        User instructor = course.getInstructor();
        LocalDateTime now = LocalDateTime.now();
        return Certificate.builder()
                .enrollment(enrollment)
                .student(student)
                .course(course)
                .studentNameSnapshot(safeValue(student.getFullName(), "Student"))
                .courseNameSnapshot(safeValue(course.getTitle(), "Course"))
                .teacherNameSnapshot(instructor == null ? "LumiNa Instructor" : safeValue(instructor.getFullName(), "LumiNa Instructor"))
                .verificationCode(generateVerificationCode())
                .status(CertificateStatus.ACTIVE)
                .issuedAt(now)
                .issueDate(now)
                .build();
    }

    private void createCertificateNotification(Certificate certificate) {
        notificationService.createNotification(certificate.getStudent(), NotificationType.CERTIFICATE_ISSUED,
                "Certificate issued",
                "Congratulations! Your certificate for " + safeValue(certificate.getCourseNameSnapshot(), "this course") + " is ready.",
                "/student/certificates",
                NotificationType.CERTIFICATE_ISSUED.name() + ":certificate:" + certificate.getId());
    }

    private Certificate findOwnedCertificate(Integer certificateId, Integer currentStudentId) {
        return certificateRepository.findByIdAndStudentId(certificateId, currentStudentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATE_NOT_FOUND));
    }

    private CertificateVerificationDTO toVerificationDto(Certificate certificate) {
        boolean active = certificate.getStatus() == CertificateStatus.ACTIVE;
        return CertificateVerificationDTO.builder()
                .valid(active)
                .status(active ? "VALID" : "REVOKED")
                .verificationCode(certificate.getVerificationCode())
                .studentNameSnapshot(certificate.getStudentNameSnapshot())
                .courseNameSnapshot(certificate.getCourseNameSnapshot())
                .teacherNameSnapshot(certificate.getTeacherNameSnapshot())
                .issuedAt(certificate.getIssuedAt())
                .certificateStatus(certificate.getStatus())
                .build();
    }

    private CertificateVerificationDTO invalidVerification(String status, String code) {
        return CertificateVerificationDTO.builder()
                .valid(false)
                .status(status)
                .verificationCode(code)
                .build();
    }

    private String normalizeVerificationCode(String verificationCode) {
        if (verificationCode == null) {
            return null;
        }
        String normalized = verificationCode.trim().toUpperCase(Locale.ROOT);
        return VERIFICATION_CODE_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private String generateVerificationCode() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase(Locale.ROOT);
    }

    private String safeValue(String value, String fallback) {
        String clean = value == null || value.isBlank() ? fallback : value;
        return clean.replaceAll("<[^>]*>", "").replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private String safePdfText(String value) {
        return safeValue(value, "");
    }

    private String formatIssuedDate(LocalDateTime issuedAt) {
        LocalDate date = issuedAt == null ? LocalDate.now() : issuedAt.toLocalDate();
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private PDFont loadUnicodeFont(PDDocument document) throws IOException {
        File windowsArial = new File("C:\\Windows\\Fonts\\arial.ttf");
        if (windowsArial.isFile()) {
            return PDType0Font.load(document, windowsArial);
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private void writeCentered(PDPageContentStream content, PDFont font, int fontSize, float y, String text) throws IOException {
        String printable = text == null ? "" : text;
        float width = font.getStringWidth(printable) / 1000 * fontSize;
        float x = Math.max(54, (PDRectangle.LETTER.getWidth() - width) / 2);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(printable);
        content.endText();
    }
}
