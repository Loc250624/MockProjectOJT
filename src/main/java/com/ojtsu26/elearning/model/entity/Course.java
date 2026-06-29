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
@Table(name = "Courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private String title;

    
    private String description;

    
    private String thumbnailUrl;

    
    private java.math.BigDecimal price;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    @JsonBackReference("course-instructor")
    private User instructor;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonBackReference("course-category")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "roadmap_id")
    @JsonBackReference("course-roadmap")
    private Roadmap roadmap;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("lesson-course")
    private List<Lesson> lessons;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("courseenrollment-course")
    private List<CourseEnrollment> courseenrollments;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("certificate-course")
    private List<Certificate> certificates;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("transaction-course")
    private List<Transaction> transactions;
}
