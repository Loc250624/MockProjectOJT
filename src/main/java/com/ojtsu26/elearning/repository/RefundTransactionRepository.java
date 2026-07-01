package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.RefundTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Integer> {
    
    @Query("SELECT r FROM RefundTransaction r WHERE " +
           "(:keyword IS NULL OR LOWER(r.refundRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<RefundTransaction> searchRefunds(@Param("keyword") String keyword, Pageable pageable);

    java.util.List<RefundTransaction> findByOrderId(Integer orderId);
}
