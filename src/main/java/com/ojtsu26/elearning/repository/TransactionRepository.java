package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    
    @EntityGraph(attributePaths = {"student", "course", "order"})
    List<Transaction> findByOrderId(Integer orderId);

    @EntityGraph(attributePaths = {"student", "course", "order"})
    Optional<Transaction> findByTransactionRef(String transactionRef);

    @EntityGraph(attributePaths = {"student", "course", "order"})
    Page<Transaction> findAll(Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.student.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    @EntityGraph(attributePaths = {"student", "course", "order"})
    Page<Transaction> searchTransactions(@Param("keyword") String keyword, Pageable pageable);
}
