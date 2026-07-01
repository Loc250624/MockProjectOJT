package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileOverviewResponseDTO {
    private String roleKey;
    private String roleLabel;
    private String identifierLabel;
    private String identifierValue;
    private String metaLabel;
    private String metaValue;
    private String advisorMessage;

    @Builder.Default
    private List<StatCard> statCards = new ArrayList<>();

    @Builder.Default
    private List<ActivityItem> activities = new ArrayList<>();

    @Builder.Default
    private List<SkillMetric> skillMetrics = new ArrayList<>();

    @Builder.Default
    private List<TeacherCourseSummary> teacherCourses = new ArrayList<>();

    @Builder.Default
    private List<TeacherStudentSummary> teacherStudents = new ArrayList<>();

    @Builder.Default
    private List<SecurityItem> securityItems = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatCard {
        private String label;
        private String value;
        private String caption;
        private String tone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String type;
        private String title;
        private String caption;
        private LocalDateTime occurredAt;
        private String link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMetric {
        private String label;
        private BigDecimal percentage;
        private String caption;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherCourseSummary {
        private Integer id;
        private String title;
        private String category;
        private String status;
        private String thumbnailUrl;
        private long enrolledStudents;
        private BigDecimal averageProgress;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherStudentSummary {
        private Integer studentId;
        private String fullName;
        private String email;
        private String avatarUrl;
        private String courseTitle;
        private BigDecimal progressPercentage;
        private Boolean completed;
        private LocalDateTime enrolledAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityItem {
        private String label;
        private String value;
        private String caption;
    }
}
