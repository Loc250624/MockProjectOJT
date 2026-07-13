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
@Table(name = "Coding_Assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String problemStatement;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCode;

    private String allowedLanguages;

    
    private Integer timeLimitMs;

    @OneToOne
    @JoinColumn(name = "lesson_id")
    @JsonBackReference("codingassignment-lesson")
    private Lesson lesson;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("testcase-assignment")
    private List<Testcase> testcases;
}
