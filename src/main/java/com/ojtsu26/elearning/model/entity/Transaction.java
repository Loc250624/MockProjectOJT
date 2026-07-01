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
@Table(name = "Transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private java.math.BigDecimal amount;

    @Convert(converter = PaymentMethodConverter.class)
    private PaymentMethod paymentMethod;

    @Column(unique = true)
    private String transactionRef;

    @Convert(converter = TransactionStatusConverter.class)
    private TransactionStatus status;

    
    private String webhookResponse;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonBackReference("transaction-student")
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonBackReference("transaction-course")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference("transaction-order")
    private Order order;
}

