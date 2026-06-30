package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
    long countByStatus(UserStatus status);
}
