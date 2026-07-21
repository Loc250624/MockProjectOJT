package com.ojtsu26.elearning.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ojtsu26.elearning.model.enums.FeedbackCategory;

import java.time.LocalDateTime;

@Entity
@Table(name = "Student_Feedback", indexes = {
        @Index(name = "idx_student_feedback_student_created", columnList = "student_id, created_at"),
        @Index(name = "idx_student_feedback_created", columnList = "created_at"),
        @Index(name = "idx_student_feedback_category", columnList = "category")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FeedbackCategory category;

    @Column(nullable = false, length = 150)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "course_content_rating", nullable = false)
    private Integer courseContentRating;

    @Column(name = "instructor_support_rating", nullable = false)
    private Integer instructorSupportRating;

    @Column(name = "learning_experience_rating", nullable = false)
    private Integer learningExperienceRating;

    @Column(name = "platform_usability_rating", nullable = false)
    private Integer platformUsabilityRating;

    @Column(name = "assessment_experience_rating", nullable = false)
    private Integer assessmentExperienceRating;

    @Column(name = "overall_satisfaction_rating", nullable = false)
    private Integer overallSatisfactionRating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonBackReference("student-feedback-student")
    private User student;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
