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
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        seedTransactions();
        seedCertificates();
        seedBlogs();
        seedComments();
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

    private void seedBlogs() {
        if (blogRepository.count() > 0) return;
        List<User> authors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TEACHER || u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
        
        if (authors.isEmpty()) return;

        BlogStatus[] statuses = BlogStatus.values();
        for (int i = 0; i < 25; i++) {
            Blog blog = Blog.builder()
                    .title(SeederUtils.getRandomBlogTitle() + " " + (i + 1))
                    .content("Full content for the blog post about educational topics.")
                    .status(SeederUtils.getRandomElement(statuses))
                    .createdAt(SeederUtils.getRandomPastDate(120))
                    .author(SeederUtils.getRandomElement(authors))
                    .build();
            blogRepository.save(blog);
        }
    }

    private void seedComments() {
        if (commentRepository.count() > 0) return;
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT).collect(Collectors.toList());
        List<Blog> blogs = blogRepository.findAll();
        
        if (students.isEmpty() || blogs.isEmpty()) return;

        for (int i = 0; i < 120; i++) {
            Blog randomBlog = SeederUtils.getRandomElement(blogs);
            
            Comment comment = Comment.builder()
                    .targetType("BLOG")
                    .targetId(randomBlog.getId())
                    .content("This is a very helpful comment!")
                    .createdAt(SeederUtils.getRandomPastDate(30))
                    .user(SeederUtils.getRandomElement(students))
                    .build();
            commentRepository.save(comment);
        }
    }
}
