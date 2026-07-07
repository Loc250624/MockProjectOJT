package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {
    Optional<SystemSetting> findByKey(String key);
    boolean existsByKey(String key);
    List<SystemSetting> findAllByOrderByCategoryAscKeyAsc();
}
