package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.CertificateEligibilityResponse;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.dto.response.CertificateVerificationDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.mapper.CertificateMapper;
import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.service.impl.CertificateServiceImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private CertificateEligibilityService eligibilityService;
    @Mock
    private CertificateMapper certificateMapper;
    @Mock
    private NotificationService notificationService;

    private CertificateServiceImpl service;
    private CourseEnrollment enrollment;
    private Certificate existing;

    @BeforeEach
    void setUp() {
        service = new CertificateServiceImpl(certificateRepository, enrollmentRepository, eligibilityService, certificateMapper, notificationService);
        User student = User.builder().id(1).fullName("Nguyen Van A").role(Role.STUDENT).build();
        User teacher = User.builder().id(2).fullName("Co Giao B").role(Role.TEACHER).build();
        Course course = Course.builder().id(10).title("Spring Boot Nang Cao").instructor(teacher).build();
        enrollment = CourseEnrollment.builder()
                .id(20)
                .student(student)
                .course(course)
                .progressPercentage(new BigDecimal("100.00"))
                .isCompleted(true)
                .build();
        existing = Certificate.builder()
                .id(30)
                .enrollment(enrollment)
                .student(student)
                .course(course)
                .studentNameSnapshot("Nguyen Van A")
                .courseNameSnapshot("Spring Boot Nang Cao")
                .teacherNameSnapshot("Co Giao B")
                .verificationCode("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456")
                .status(CertificateStatus.ACTIVE)
                .issuedAt(LocalDateTime.now())
                .build();
        lenient().when(certificateMapper.toDto(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate certificate = invocation.getArgument(0);
            CertificateResponseDTO dto = new CertificateResponseDTO();
            dto.setId(certificate.getId());
            dto.setEnrollmentId(certificate.getEnrollment().getId());
            dto.setCourseId(certificate.getCourse().getId());
            dto.setStudentName(certificate.getStudentNameSnapshot());
            dto.setCourseName(certificate.getCourseNameSnapshot());
            dto.setTeacherName(certificate.getTeacherNameSnapshot());
            dto.setVerificationCode(certificate.getVerificationCode());
            dto.setStatus(certificate.getStatus());
            return dto;
        });
    }

    @Test
    void issueIfEligibleCreatesSnapshotAndNotificationOnce() {
        when(enrollmentRepository.findByIdForCertificateIssue(20)).thenReturn(Optional.of(enrollment));
        when(certificateRepository.findByEnrollmentId(20)).thenReturn(Optional.empty());
        when(eligibilityService.evaluateEligibility(20, 1)).thenReturn(eligible());
        when(certificateRepository.saveAndFlush(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate certificate = invocation.getArgument(0);
            certificate.setId(30);
            return certificate;
        });

        CertificateResponseDTO response = service.issueIfEligible(20, 1);

        assertEquals(30, response.getId());
        assertEquals("Nguyen Van A", response.getStudentName());
        assertEquals("Spring Boot Nang Cao", response.getCourseName());
        assertEquals("Co Giao B", response.getTeacherName());
        assertNotEquals("30", response.getVerificationCode());
        verify(notificationService).createNotification(eq(enrollment.getStudent()), eq(NotificationType.CERTIFICATE_ISSUED),
                anyString(), contains("Spring Boot Nang Cao"), eq("/student/certificates"), eq("CERTIFICATE_ISSUED:certificate:30"));
    }

    @Test
    void issueIfEligibleReturnsExistingCertificateWithoutDuplicateNotification() {
        when(enrollmentRepository.findByIdForCertificateIssue(20)).thenReturn(Optional.of(enrollment));
        when(certificateRepository.findByEnrollmentId(20)).thenReturn(Optional.of(existing));

        CertificateResponseDTO response = service.issueIfEligible(20, 1);

        assertEquals(30, response.getId());
        verify(certificateRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void notEligibleDoesNotCreateCertificate() {
        when(enrollmentRepository.findByIdForCertificateIssue(20)).thenReturn(Optional.of(enrollment));
        when(certificateRepository.findByEnrollmentId(20)).thenReturn(Optional.empty());
        when(eligibilityService.evaluateEligibility(20, 1)).thenReturn(CertificateEligibilityResponse.builder()
                .eligible(false)
                .unmetRequirements(List.of("Required assessments are not fully passed."))
                .build());

        assertThrows(BusinessException.class, () -> service.issueIfEligible(20, 1));
        verify(certificateRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateRaceReturnsPersistedCertificate() {
        when(enrollmentRepository.findByIdForCertificateIssue(20)).thenReturn(Optional.of(enrollment));
        when(certificateRepository.findByEnrollmentId(20)).thenReturn(Optional.empty(), Optional.of(existing));
        when(eligibilityService.evaluateEligibility(20, 1)).thenReturn(eligible());
        when(certificateRepository.saveAndFlush(any(Certificate.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        CertificateResponseDTO response = service.issueIfEligible(20, 1);

        assertEquals(30, response.getId());
        verifyNoInteractions(notificationService);
    }

    @Test
    void verifyRevokedCodeIsNotValidAndDoesNotExposeIds() {
        existing.setStatus(CertificateStatus.REVOKED);
        when(certificateRepository.findByVerificationCodeIgnoreCase("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456")).thenReturn(Optional.of(existing));

        CertificateVerificationDTO response = service.verifyByCode("abcdefghijklmnopqrstuvwxyz123456");

        assertFalse(response.isValid());
        assertEquals("REVOKED", response.getStatus());
        assertEquals("Nguyen Van A", response.getStudentNameSnapshot());
    }

    @Test
    void ownedPdfCanBeGenerated() throws Exception {
        when(certificateRepository.findByIdAndStudentId(30, 1)).thenReturn(Optional.of(existing));

        byte[] pdf = service.generateCertificatePdf(30, 1);

        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            PDRectangle mediaBox = document.getPage(0).getMediaBox();
            assertTrue(mediaBox.getWidth() > mediaBox.getHeight());
            assertEquals(PDRectangle.A4.getHeight(), mediaBox.getWidth(), 1.0f);
            assertEquals(PDRectangle.A4.getWidth(), mediaBox.getHeight(), 1.0f);

            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Nguyen Van A"));
            assertTrue(text.contains("Spring Boot Nang Cao"));
            assertTrue(text.contains("Co Giao B"));
            assertTrue(text.contains("Completed:"));
            assertTrue(text.replaceAll("\\s+", "").contains("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456"));
            assertTrue(text.contains("Verification Code:"));
            assertFalse(text.contains("Issued:"));
            assertFalse(text.contains("Verify at"));

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 120);
            Path renderDir = Path.of("target", "pdf-render");
            Files.createDirectories(renderDir);
            Files.write(renderDir.resolve("certificate-service-test.pdf"), pdf);
            assertTrue(ImageIO.write(image, "png", renderDir.resolve("certificate-service-test.png").toFile()));
        }
    }

    @Test
    void generatedPdfKeepsUnicodeAndLongDynamicTextSelectable() throws Exception {
        existing.setStudentNameSnapshot("Nguy\u1ec5n M\u1ea1nh C\u01b0\u1eddng Qu\u1ed1c Anh Tr\u1ea7n Ho\u00e0ng Minh");
        existing.setCourseNameSnapshot("L\u1eadp tr\u00ecnh h\u01b0\u1edbng \u0111\u1ed1i t\u01b0\u1ee3ng n\u00e2ng cao v\u1edbi Java, ki\u1ec3m th\u1eed t\u1ef1 \u0111\u1ed9ng v\u00e0 thi\u1ebft k\u1ebf ph\u1ea7n m\u1ec1m b\u1ec1n v\u1eefng");
        existing.setTeacherNameSnapshot("Tr\u1ea7n Th\u1ecb \u00c1nh");
        existing.setVerificationCode("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_LONG_CODE");
        existing.setIssuedAt(LocalDateTime.of(2026, 1, 15, 10, 30));
        when(certificateRepository.findByIdAndStudentId(30, 1)).thenReturn(Optional.of(existing));

        byte[] pdf = service.generateCertificatePdf(30, 1);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getPage(0).getResources().getXObjectNames().iterator().hasNext());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Nguy\u1ec5n M\u1ea1nh C\u01b0\u1eddng"));
            assertTrue(text.contains("L\u1eadp tr\u00ecnh h\u01b0\u1edbng \u0111\u1ed1i t\u01b0\u1ee3ng"));
            assertTrue(text.contains("Tr\u1ea7n Th\u1ecb \u00c1nh"));
            assertTrue(text.contains("Completed: 2026-01-15"));
            assertTrue(text.replaceAll("\\s+", "").contains("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_LONG_CODE"));
            assertFalse(text.contains("Issued:"));
            assertFalse(text.contains("Verify at"));
        }
    }

    private CertificateEligibilityResponse eligible() {
        return CertificateEligibilityResponse.builder()
                .eligible(true)
                .unmetRequirements(List.of())
                .build();
    }
}
