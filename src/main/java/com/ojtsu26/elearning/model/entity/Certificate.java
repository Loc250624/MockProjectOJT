package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ojtsu26.elearning.model.enums.*;

@Entity
@Table(name = "Certificates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_certificate_enrollment", columnNames = "enrollment_id"),
    @UniqueConstraint(name = "uk_certificate_verification_code", columnNames = "verification_code")
}, indexes = {
    @Index(name = "idx_certificate_student_issued", columnList = "student_id, issued_at"),
    @Index(name = "idx_certificate_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private java.time.LocalDateTime issuedAt;

    @Deprecated
    private java.time.LocalDateTime issueDate;

    @Deprecated
    private String certificateUrl;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    @JsonBackReference("certificate-enrollment")
    private CourseEnrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @JsonBackReference("certificate-student")
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference("certificate-course")
    private Course course;

    @Column(name = "student_name_snapshot", nullable = false, length = 160)
    private String studentNameSnapshot;

    @Column(name = "course_name_snapshot", nullable = false, length = 240)
    private String courseNameSnapshot;

    @Column(name = "teacher_name_snapshot", nullable = false, length = 160)
    private String teacherNameSnapshot;

    @Column(name = "verification_code", nullable = false, length = 64)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CertificateStatus status;

    private java.time.LocalDateTime revokedAt;

    @Column(length = 500)
    private String revokedReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private java.time.LocalDateTime updatedAt;
}
