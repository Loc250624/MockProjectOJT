package com.ojtsu26.elearning.model.entity;

import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Code_Judge_Results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeJudgeResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Convert(converter = CodeJudgeStatusConverter.class)
    private CodeJudgeStatus status;

    private Integer totalTests;

    private Integer passedTests;

    @Column(columnDefinition = "TEXT")
    private String outputLog;

    private Long executionTimeMs;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
