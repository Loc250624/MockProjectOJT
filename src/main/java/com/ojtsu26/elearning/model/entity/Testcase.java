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
@Table(name = "Testcases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Testcase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private String inputData;

    
    private String expectedOutput;

    
    private Boolean isHidden;

    private java.math.BigDecimal points;

    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    @JsonBackReference("testcase-assignment")
    private CodingAssignment assignment;
}
