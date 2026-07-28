package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ojtsu26.elearning.model.enums.*;
import java.util.List;

@Entity
@Table(name = "Lessons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private String title;

    
    private String content;

    @Convert(converter = LessonTypeConverter.class)
    private LessonType type;

    
    private Integer orderIndex;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonBackReference("lesson-course")
    private Course course;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("video-lesson")
    private Video video;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("quiz-lesson")
    private Quiz quiz;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("lessonprogress-lesson")
    private List<LessonProgress> lessonprogresss;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("submission-lesson")
    private List<Submission> submissions;
}
