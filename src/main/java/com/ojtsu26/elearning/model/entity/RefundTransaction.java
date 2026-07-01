package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.ojtsu26.elearning.model.enums.RefundStatus;
import com.ojtsu26.elearning.model.enums.RefundStatusConverter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Refund_Transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private BigDecimal amount;

    @Convert(converter = RefundStatusConverter.class)
    private RefundStatus status;

    private String refundRef;

    private String providerRefundId;

    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;
}
