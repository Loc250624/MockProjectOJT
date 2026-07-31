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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{20,64}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final PDRectangle CERTIFICATE_PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final Color CERT_NAVY = new Color(23, 39, 65);
    private static final Color CERT_NAVY_SOFT = new Color(49, 68, 99);
    private static final Color CERT_GOLD = new Color(197, 138, 28);
    private static final Color CERT_PAPER = new Color(251, 251, 252);
    private static final String CERTIFICATE_SEAL_RESOURCE = "/static/images/certificate-seal.png";
    private static final float CERTIFICATE_SEAL_SIZE = 112f;

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
            PDPage page = new PDPage(CERTIFICATE_PAGE);
            document.addPage(page);
            PDFont regular = loadFont(document, "arial.ttf", Standard14Fonts.FontName.HELVETICA,
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/Library/Fonts/Arial Unicode.ttf");
            PDFont bold = loadFont(document, "arialbd.ttf", Standard14Fonts.FontName.HELVETICA_BOLD,
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                    "/Library/Fonts/Arial Bold.ttf");
            PDFont display = loadFont(document, "times.ttf", Standard14Fonts.FontName.TIMES_ROMAN,
                    "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                    "/Library/Fonts/Times New Roman.ttf");
            PDFont displayBold = loadFont(document, "timesbd.ttf", Standard14Fonts.FontName.TIMES_BOLD,
                    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
                    "/Library/Fonts/Times New Roman Bold.ttf");
            PDImageXObject certificateSeal = loadCertificateSeal(document);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawPaper(content);
                drawCertificateBorder(content);

                drawWrappedText(content, regular, 8f, 58, 520, 285, 10,
                        "Verification Code: " + safePdfText(certificate.getVerificationCode()), 2);

                drawTrackingCentered(content, displayBold, 45f, 444, "LUMINA", 7f);
                drawTrackingCentered(content, bold, 11f, 407, "E-LEARNING PLATFORM", 6f);
                writeCentered(content, regular, 15, 354, "Certificate of Completion");

                String studentName = safePdfText(certificate.getStudentNameSnapshot());
                float nameSize = fitFontSize(display, studentName, 540, 42, 24);
                List<String> nameLines = wrapText(display, nameSize, studentName, 540, 2);
                float firstNameY = nameLines.size() == 1 ? 292 : 308;
                float nameY = firstNameY;
                for (String line : nameLines) {
                    writeCentered(content, display, nameSize, nameY, line);
                    nameY -= nameSize * 1.08f;
                }
                float underlineY = nameY + (nameSize * 0.08f) - 10;
                drawCenteredLine(content, underlineY, 390, CERT_GOLD, 0.8f);

                writeCentered(content, regular, 14, underlineY - 34, "has successfully completed the course");
                String courseTitle = safePdfText(certificate.getCourseNameSnapshot());
                float courseSize = fitFontSize(bold, courseTitle, 560, 19, 12);
                List<String> courseLines = wrapText(bold, courseSize, courseTitle, 560, 2);
                float courseY = underlineY - 58;
                for (String line : courseLines) {
                    writeCentered(content, bold, courseSize, courseY, line);
                    courseY -= courseSize * 1.22f;
                }
                writeCentered(content, regular, 11.5f, courseY - 8,
                        "Completed: " + formatCompletionDate(certificate.getIssuedAt()));

                drawFooter(content, regular, bold, safePdfText(certificate.getTeacherNameSnapshot()),
                        certificateSeal);
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

    private String formatCompletionDate(LocalDateTime issuedAt) {
        LocalDate date = issuedAt == null ? LocalDate.now() : issuedAt.toLocalDate();
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private PDFont loadFont(PDDocument document, String windowsFontFile, Standard14Fonts.FontName fallback,
                            String... additionalCandidates) throws IOException {
        File fontFile = new File("C:\\Windows\\Fonts\\" + windowsFontFile);
        if (fontFile.isFile()) {
            return PDType0Font.load(document, fontFile);
        }
        for (String candidate : additionalCandidates) {
            File candidateFile = new File(candidate);
            if (candidateFile.isFile()) {
                return PDType0Font.load(document, candidateFile);
            }
        }
        return new PDType1Font(fallback);
    }

    private PDImageXObject loadCertificateSeal(PDDocument document) throws IOException {
        try (InputStream input = CertificateServiceImpl.class.getResourceAsStream(CERTIFICATE_SEAL_RESOURCE)) {
            if (input == null) {
                throw new IOException("Certificate seal resource is unavailable.");
            }
            return PDImageXObject.createFromByteArray(document, input.readAllBytes(), "certificate-seal");
        }
    }

    private void drawPaper(PDPageContentStream content) throws IOException {
        content.setNonStrokingColor(CERT_PAPER);
        content.addRect(0, 0, CERTIFICATE_PAGE.getWidth(), CERTIFICATE_PAGE.getHeight());
        content.fill();
    }

    private void drawCertificateBorder(PDPageContentStream content) throws IOException {
        float width = CERTIFICATE_PAGE.getWidth();
        float height = CERTIFICATE_PAGE.getHeight();

        content.setStrokingColor(CERT_NAVY);
        content.setLineWidth(3.5f);
        content.addRect(36, 34, width - 72, height - 68);
        content.stroke();

        content.setLineWidth(0.9f);
        content.addRect(49, 47, width - 98, height - 94);
        content.stroke();

        content.setLineWidth(1.1f);
        content.moveTo(70, height - 62);
        content.lineTo(320, height - 62);
        content.moveTo(width - 320, height - 62);
        content.lineTo(width - 70, height - 62);
        content.moveTo(70, 62);
        content.lineTo(320, 62);
        content.moveTo(width - 320, 62);
        content.lineTo(width - 70, 62);
        content.stroke();

        content.setLineWidth(2.2f);
        drawCornerAccent(content, 36, 34, 1, 1);
        drawCornerAccent(content, width - 36, 34, -1, 1);
        drawCornerAccent(content, 36, height - 34, 1, -1);
        drawCornerAccent(content, width - 36, height - 34, -1, -1);
        content.stroke();
    }

    private void drawCornerAccent(PDPageContentStream content, float cornerX, float cornerY, int horizontal, int vertical) throws IOException {
        float inset = 28;
        float radius = 20;
        float startX = cornerX + horizontal * inset;
        float startY = cornerY;
        float endX = cornerX;
        float endY = cornerY + vertical * inset;
        float control = radius * 0.55f;
        content.moveTo(startX, startY);
        content.curveTo(startX, startY + vertical * control, endX + horizontal * control, endY, endX, endY);
    }

    private void drawFooter(PDPageContentStream content, PDFont regular, PDFont bold, String instructorName,
                            PDImageXObject certificateSeal) throws IOException {
        drawSignature(content, regular, bold, 180, 110, instructorName, "Instructor");
        float sealX = (CERTIFICATE_PAGE.getWidth() - CERTIFICATE_SEAL_SIZE) / 2;
        content.drawImage(certificateSeal, sealX, 40, CERTIFICATE_SEAL_SIZE, CERTIFICATE_SEAL_SIZE);
        drawSignature(content, regular, bold, CERTIFICATE_PAGE.getWidth() - 180, 110, "LUMINA E-LEARNING", "Learning Platform");
    }

    private void drawSignature(PDPageContentStream content, PDFont regular, PDFont bold, float centerX, float lineY,
                               String name, String role) throws IOException {
        drawLine(content, centerX - 80, lineY, centerX + 80, lineY, CERT_GOLD, 0.8f);
        writeCenteredAt(content, bold, 9.5f, centerX, lineY - 23, safePdfText(name));
        drawTrackingCenteredAt(content, regular, 8f, centerX, lineY - 40, role, 2f);
    }

    private void drawCenteredLine(PDPageContentStream content, float y, float width, Color color, float lineWidth) throws IOException {
        float pageCenter = CERTIFICATE_PAGE.getWidth() / 2;
        drawLine(content, pageCenter - width / 2, y, pageCenter + width / 2, y, color, lineWidth);
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2, Color color, float lineWidth) throws IOException {
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
        content.setStrokingColor(CERT_NAVY);
    }

    private void writeCentered(PDPageContentStream content, PDFont font, float fontSize, float y, String text) throws IOException {
        writeCenteredAt(content, font, fontSize, CERTIFICATE_PAGE.getWidth() / 2, y, text);
    }

    private void writeCenteredAt(PDPageContentStream content, PDFont font, float fontSize, float centerX, float y, String text) throws IOException {
        String printable = text == null ? "" : text;
        float width = font.getStringWidth(printable) / 1000 * fontSize;
        float x = Math.max(54, centerX - width / 2);
        drawTextAt(content, font, fontSize, x, y, printable);
    }

    private void drawTextAt(PDPageContentStream content, PDFont font, float fontSize, float x, float y, String text) throws IOException {
        content.setNonStrokingColor(CERT_NAVY);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
    }

    private void drawTrackingCentered(PDPageContentStream content, PDFont font, float fontSize, float y, String text, float tracking) throws IOException {
        drawTrackingCenteredAt(content, font, fontSize, CERTIFICATE_PAGE.getWidth() / 2, y, text, tracking);
    }

    private void drawTrackingCenteredAt(PDPageContentStream content, PDFont font, float fontSize, float centerX,
                                        float y, String text, float tracking) throws IOException {
        String printable = text == null ? "" : text;
        float width = trackedTextWidth(font, fontSize, printable, tracking);
        float x = centerX - width / 2;
        content.setNonStrokingColor(CERT_NAVY);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        for (int i = 0; i < printable.length(); i++) {
            String character = printable.substring(i, i + 1);
            content.showText(character);
            if (i < printable.length() - 1) {
                float advance = textWidth(font, fontSize, character) + tracking;
                content.newLineAtOffset(advance, 0);
            }
        }
        content.endText();
    }

    private float trackedTextWidth(PDFont font, float fontSize, String text, float tracking) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return textWidth(font, fontSize, text) + tracking * Math.max(0, text.length() - 1);
    }

    private float fitFontSize(PDFont font, String text, float maxWidth, float maxSize, float minSize) throws IOException {
        String printable = text == null ? "" : text;
        float size = maxSize;
        while (size > minSize && textWidth(font, size, printable) > maxWidth) {
            size -= 0.5f;
        }
        return Math.max(minSize, size);
    }

    private float textWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text == null ? "" : text) / 1000f * fontSize;
    }

    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth, int maxLines) throws IOException {
        String printable = text == null ? "" : text.trim();
        List<String> lines = new ArrayList<>();
        if (printable.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder current = new StringBuilder();
        String[] words = printable.split("\\s+");
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(font, fontSize, candidate) <= maxWidth || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        while (lines.size() > maxLines) {
            int last = maxLines - 1;
            String merged = lines.get(last) + " " + lines.remove(last + 1);
            lines.set(last, merged);
        }
        return lines;
    }

    private void drawWrappedText(PDPageContentStream content, PDFont font, float fontSize, float x, float y,
                                 float maxWidth, float lineHeight, String text, int maxLines) throws IOException {
        List<String> lines = wrapText(font, fontSize, text, maxWidth, maxLines);
        float currentY = y;
        content.setNonStrokingColor(CERT_NAVY_SOFT);
        for (String line : lines) {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, currentY);
            content.showText(line);
            content.endText();
            currentY -= lineHeight;
        }
        content.setNonStrokingColor(CERT_NAVY);
    }
}
