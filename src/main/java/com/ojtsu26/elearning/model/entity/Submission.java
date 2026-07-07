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
@Table(name = "Submissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private java.math.BigDecimal score;

    @Convert(converter = SubmissionStatusConverter.class)
    private SubmissionStatus status;

    
    private String submittedContent;

    private String codeLanguage;

    @Column(columnDefinition = "TEXT")
    private String codeContent;

    private String filePath;

    
    private String teacherFeedback;

    @CreationTimestamp
    private java.time.LocalDateTime submittedAt;

    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonBackReference("submission-student")
    private User student;

    @ManyToOne
    @JoinColumn(name = "lesson_id")
    @JsonBackReference("submission-lesson")
    private Lesson lesson;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private CodingAssignment assignment;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodeJudgeResult> judgeResults;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GradeFeedback> gradeFeedbacks;
}
