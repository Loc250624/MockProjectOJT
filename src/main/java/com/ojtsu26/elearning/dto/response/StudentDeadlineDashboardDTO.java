package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDeadlineDashboardDTO {
    private Integer id;
    private String type;
    private String title;
    private Integer courseId;
    private String courseTitle;
    private LocalDateTime dueAt;
    private String day;
    private String month;
    private String statusLabel;
    private String statusClass;
    private String cardClass;
    private String destinationUrl;
}
