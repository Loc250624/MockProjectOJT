package com.ojtsu26.elearning.seeder;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BusinessDataSeeder {

    private final TransactionRepository transactionRepository;
    private final CertificateRepository certificateRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Transactional
    public void seed() {
        seedTransactions();
        seedCertificates();
    }

    private void seedTransactions() {
        if (transactionRepository.count() > 0) return;
        List<CourseEnrollment> enrollments = enrollmentRepository.findAll();
        
        int transactionCount = 0;
        int targetTransactions = 90; // between 60-120
        TransactionStatus[] statuses = TransactionStatus.values();
        PaymentMethod[] methods = PaymentMethod.values();

        for (CourseEnrollment enrollment : enrollments) {
            if (transactionCount >= targetTransactions) break;
            
            Transaction transaction = Transaction.builder()
                    .amount(enrollment.getCourse().getPrice())
                    .paymentMethod(SeederUtils.getRandomElement(methods))
                    .transactionRef(UUID.randomUUID().toString())
                    .status(SeederUtils.getRandomElement(statuses))
                    .webhookResponse("{}")
                    .createdAt(SeederUtils.getRandomPastDate(60))
                    .student(enrollment.getStudent())
                    .course(enrollment.getCourse())
                    .build();
            transactionRepository.save(transaction);
            transactionCount++;
        }
    }

    private void seedCertificates() {
        if (certificateRepository.count() > 0) return;
        List<CourseEnrollment> completedEnrollments = enrollmentRepository.findAll().stream()
                .filter(CourseEnrollment::getIsCompleted)
                .collect(Collectors.toList());

        int certificateCount = 0;
        int targetCertificates = 40; // between 30-60

        for (CourseEnrollment enrollment : completedEnrollments) {
            if (certificateCount >= targetCertificates) break;
            if (enrollment.getStudent() == null || enrollment.getCourse() == null
                    || certificateRepository.existsByEnrollmentId(enrollment.getId())) {
                continue;
            }
            User instructor = enrollment.getCourse().getInstructor();
            
            Certificate certificate = Certificate.builder()
                    .enrollment(enrollment)
                    .issueDate(SeederUtils.getRandomPastDate(10))
                    .issuedAt(SeederUtils.getRandomPastDate(10))
                    .certificateUrl("https://example.com/certificates/" + UUID.randomUUID() + ".pdf")
                    .student(enrollment.getStudent())
                    .course(enrollment.getCourse())
                    .studentNameSnapshot(enrollment.getStudent().getFullName())
                    .courseNameSnapshot(enrollment.getCourse().getTitle())
                    .teacherNameSnapshot(instructor == null ? "LumiNa Instructor" : instructor.getFullName())
                    .verificationCode(UUID.randomUUID().toString().replace("-", "").toUpperCase())
                    .status(CertificateStatus.ACTIVE)
                    .build();
            certificateRepository.save(certificate);
            certificateCount++;
        }
    }

}
