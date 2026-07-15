package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudentAnalyticsDTO {
    private LocalDate from;
    private LocalDate to;
    private String timeZone;
    private String groupBy;
    private long newStudents;
    private long activeStudents;
    private String activeStudentMethod;
    private List<AdminStudentAnalyticsPointDTO> trend;
}
