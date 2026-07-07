package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByStatusAndExpiredAtBefore(OrderStatus status, LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :studentId " +
           "AND EXISTS (SELECT oi FROM OrderItem oi WHERE oi.order.id = o.id AND oi.course.id = :courseId) " +
           "AND o.status = :status")
    List<Order> findByStudentIdAndCourseIdAndStatus(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId,
            @Param("status") OrderStatus status
    );

    @Query("SELECT oi.course.id FROM Order o JOIN o.items oi WHERE o.user.id = :studentId AND o.status = :status")
    List<Integer> findCourseIdsByStudentIdAndStatus(
            @Param("studentId") Integer studentId,
            @Param("status") OrderStatus status
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    Page<Order> findByUserId(Integer userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:keyword IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
}
