package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.UserProviderIdentity;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProviderIdentityRepository extends JpaRepository<UserProviderIdentity, Integer> {
    Optional<UserProviderIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
    Optional<UserProviderIdentity> findByUserIdAndProvider(Integer userId, AuthProvider provider);
}
