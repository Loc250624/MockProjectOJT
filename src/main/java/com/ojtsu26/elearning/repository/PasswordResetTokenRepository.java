package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.PasswordResetToken;
import com.ojtsu26.elearning.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
}
