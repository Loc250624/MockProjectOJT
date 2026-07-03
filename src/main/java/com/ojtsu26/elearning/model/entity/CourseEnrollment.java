package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ojtsu26.elearning.model.enums.*;
import java.util.List;

@Entity
@Table(name = "Course_Enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "course_id"})
}, indexes = {
    @Index(name = "idx_course_enrollments_course", columnList = "course_id"),
    @Index(name = "idx_course_enrollments_student", columnList = "student_id"),
    @Index(name = "idx_course_enrollments_course_enrolled", columnList = "course_id,enrolledAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private java.math.BigDecimal progressPercentage;

    
    private Boolean isCompleted;

    @CreationTimestamp
    private java.time.LocalDateTime enrolledAt;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonBackReference("courseenrollment-student")
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonBackReference("courseenrollment-course")
    private Course course;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("lessonprogress-enrollment")
    private List<LessonProgress> lessonprogresss;

    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("certificate-enrollment")
    private Certificate certificate;
}
