package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.projection.AdminStudentEventProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
    long countByRole(Role role);
    long countByRoleAndStatus(Role role, UserStatus status);
    long countByStatus(UserStatus status);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role role, LocalDateTime from, LocalDateTime to);
    List<User> findTop5ByOrderByCreatedAtDesc();
    List<User> findByRole(Role role);

    @Query("""
            select u.id as studentId,
                   u.createdAt as occurredAt
            from User u
            where u.role = :studentRole
              and u.createdAt >= :from
              and u.createdAt < :to
            order by u.createdAt asc, u.id asc
            """)
    List<AdminStudentEventProjection> findNewStudentEvents(@Param("studentRole") Role studentRole,
                                                           @Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to);
}
