package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
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

    long countByStatus(TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = :status")
    java.math.BigDecimal sumAmountByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    long countInCreatedAtRange(@Param("fromDate") LocalDateTime fromDate,
                               @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.status = :status " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    long countByStatusInCreatedAtRange(@Param("status") TransactionStatus status,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.status = :status " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    java.math.BigDecimal sumAmountByStatusInCreatedAtRange(@Param("status") TransactionStatus status,
                                                           @Param("fromDate") LocalDateTime fromDate,
                                                           @Param("toDate") LocalDateTime toDate);

    @EntityGraph(attributePaths = {"student", "course", "order"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findAdminDetailById(@Param("id") Integer id);

    @Query(value = "SELECT t FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    @EntityGraph(attributePaths = {"student", "course", "order"})
    Page<Transaction> searchTransactions(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT t FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    @EntityGraph(attributePaths = {"student", "course", "order"})
    Page<Transaction> searchTransactionsInCreatedAtRange(@Param("keyword") String keyword,
                                                         @Param("fromDate") LocalDateTime fromDate,
                                                         @Param("toDate") LocalDateTime toDate,
                                                         Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    @EntityGraph(attributePaths = {"student", "course", "order"})
    List<Transaction> findTransactionsForAdminPaymentsExport(@Param("keyword") String keyword,
                                                             @Param("fromDate") LocalDateTime fromDate,
                                                             @Param("toDate") LocalDateTime toDate,
                                                             org.springframework.data.domain.Sort sort);

    @Query(value = "SELECT t FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN t.student s " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(t.transactionRef) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt < :toDate)")
    @EntityGraph(attributePaths = {"student", "course", "order"})
    Page<Transaction> searchAdminTransactions(@Param("keyword") String keyword,
                                              @Param("status") TransactionStatus status,
                                              @Param("paymentMethod") PaymentMethod paymentMethod,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate,
                                              Pageable pageable);
}
