package com.ojtsu26.elearning.model.entity;

import com.ojtsu26.elearning.model.enums.SystemSettingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SystemSettings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 120)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 1000)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SystemSettingType type;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean editable;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
