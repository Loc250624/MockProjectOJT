package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.OrderItem;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.repository.projection.AdminRevenueBucketProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueCourseProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueEventProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("""
            select coalesce(sum(oi.unitPrice), 0)
            from OrderItem oi
            join oi.order o
            where o.status = :paidStatus
            """)
    BigDecimal sumPaidRevenue(@Param("paidStatus") OrderStatus paidStatus);

    @Query("""
            select coalesce(sum(oi.unitPrice), 0)
            from OrderItem oi
            join oi.order o
            where o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
            """)
    BigDecimal sumPaidRevenueForPeriod(@Param("paidStatus") OrderStatus paidStatus,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    @Query("""
            select year(o.createdAt) as bucketYear,
                   month(o.createdAt) as bucketMonth,
                   day(o.createdAt) as bucketDay,
                   coalesce(sum(oi.unitPrice), 0) as revenue,
                   count(distinct o.id) as paidOrderCount
            from OrderItem oi
            join oi.order o
            where o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
            group by year(o.createdAt), month(o.createdAt), day(o.createdAt)
            order by year(o.createdAt) asc, month(o.createdAt) asc, day(o.createdAt) asc
            """)
    List<AdminRevenueBucketProjection> findPaidRevenueBucketsByDay(@Param("paidStatus") OrderStatus paidStatus,
                                                                    @Param("from") LocalDateTime from,
                                                                    @Param("to") LocalDateTime to);

    @Query("""
            select year(o.createdAt) as bucketYear,
                   month(o.createdAt) as bucketMonth,
                   null as bucketDay,
                   coalesce(sum(oi.unitPrice), 0) as revenue,
                   count(distinct o.id) as paidOrderCount
            from OrderItem oi
            join oi.order o
            where o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
            group by year(o.createdAt), month(o.createdAt)
            order by year(o.createdAt) asc, month(o.createdAt) asc
            """)
    List<AdminRevenueBucketProjection> findPaidRevenueBucketsByMonth(@Param("paidStatus") OrderStatus paidStatus,
                                                                      @Param("from") LocalDateTime from,
                                                                      @Param("to") LocalDateTime to);

    @Query("""
            select year(o.createdAt) as bucketYear,
                   null as bucketMonth,
                   null as bucketDay,
                   coalesce(sum(oi.unitPrice), 0) as revenue,
                   count(distinct o.id) as paidOrderCount
            from OrderItem oi
            join oi.order o
            where o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
            group by year(o.createdAt)
            order by year(o.createdAt) asc
            """)
    List<AdminRevenueBucketProjection> findPaidRevenueBucketsByYear(@Param("paidStatus") OrderStatus paidStatus,
                                                                     @Param("from") LocalDateTime from,
                                                                     @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(oi.unitPrice), 0)
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and (:courseId is null or c.id = :courseId)
            """)
    BigDecimal sumTeacherRevenue(@Param("teacherId") Integer teacherId,
                                 @Param("paidStatus") OrderStatus paidStatus,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("courseId") Integer courseId);

    @Query("""
            select count(distinct o.id)
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and (:courseId is null or c.id = :courseId)
            """)
    long countTeacherPaidOrders(@Param("teacherId") Integer teacherId,
                                @Param("paidStatus") OrderStatus paidStatus,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                @Param("courseId") Integer courseId);

    @Query("""
            select count(distinct o.user.id)
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and (:courseId is null or c.id = :courseId)
            """)
    long countTeacherPaidStudents(@Param("teacherId") Integer teacherId,
                                  @Param("paidStatus") OrderStatus paidStatus,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("courseId") Integer courseId);

    @Query("""
            select o.id as orderId,
                   o.createdAt as createdAt,
                   c.id as courseId,
                   c.title as courseTitle,
                   oi.unitPrice as revenue
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and (:courseId is null or c.id = :courseId)
            order by o.createdAt asc, o.id asc
            """)
    List<TeacherRevenueEventProjection> findTeacherRevenueEvents(@Param("teacherId") Integer teacherId,
                                                                 @Param("paidStatus") OrderStatus paidStatus,
                                                                 @Param("from") LocalDateTime from,
                                                                 @Param("to") LocalDateTime to,
                                                                 @Param("courseId") Integer courseId);

    @Query("""
            select c.id as courseId,
                   c.title as courseTitle,
                   coalesce(sum(oi.unitPrice), 0) as revenue,
                   count(distinct o.id) as paidOrderCount,
                   count(oi.id) as unitsSold
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and (:courseId is null or c.id = :courseId)
            group by c.id, c.title
            order by coalesce(sum(oi.unitPrice), 0) desc, count(oi.id) desc, c.title asc
            """)
    List<TeacherRevenueCourseProjection> findTeacherRevenueByCourse(@Param("teacherId") Integer teacherId,
                                                                    @Param("paidStatus") OrderStatus paidStatus,
                                                                    @Param("from") LocalDateTime from,
                                                                    @Param("to") LocalDateTime to,
                                                                    @Param("courseId") Integer courseId);

    @Query("""
            select distinct o.currency
            from OrderItem oi
            join oi.order o
            join oi.course c
            where c.instructor.id = :teacherId
              and o.status = :paidStatus
              and o.createdAt >= :from
              and o.createdAt < :to
              and o.currency is not null
              and o.currency <> ''
            """)
    List<String> findTeacherPaidRevenueCurrencies(@Param("teacherId") Integer teacherId,
                                                  @Param("paidStatus") OrderStatus paidStatus,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);
}
