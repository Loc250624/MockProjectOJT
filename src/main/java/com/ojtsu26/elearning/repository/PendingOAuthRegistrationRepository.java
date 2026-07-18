package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.PendingOAuthRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingOAuthRegistrationRepository extends JpaRepository<PendingOAuthRegistration, Integer> {
    Optional<PendingOAuthRegistration> findByTokenAndUsedAtIsNull(String token);
}
