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
@Table(name = "Lesson_Progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"enrollment_id", "lesson_id"})
}, indexes = {
    @Index(name = "idx_lesson_progress_enrollment", columnList = "enrollment_id"),
    @Index(name = "idx_lesson_progress_lesson", columnList = "lesson_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private Boolean isCompleted;

    
    private java.time.LocalDateTime completedAt;

    private java.time.LocalDateTime lastAccessedAt;

    private Integer watchedSeconds;

    @UpdateTimestamp
    private java.time.LocalDateTime lastUpdatedAt;

    @ManyToOne
    @JoinColumn(name = "enrollment_id")
    @JsonBackReference("lessonprogress-enrollment")
    private CourseEnrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "lesson_id")
    @JsonBackReference("lessonprogress-lesson")
    private Lesson lesson;
}
