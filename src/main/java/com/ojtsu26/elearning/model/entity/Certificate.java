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
@Table(name = "Certificates")
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
    private java.time.LocalDateTime issueDate;

    
    private String certificateUrl;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonBackReference("certificate-student")
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonBackReference("certificate-course")
    private Course course;
}
