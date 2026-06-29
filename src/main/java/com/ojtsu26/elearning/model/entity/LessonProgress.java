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
@Table(name = "Lesson_Progress")
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

    @ManyToOne
    @JoinColumn(name = "enrollment_id")
    @JsonBackReference("lessonprogress-enrollment")
    private CourseEnrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "lesson_id")
    @JsonBackReference("lessonprogress-lesson")
    private Lesson lesson;
}
