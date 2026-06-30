package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileOverviewDTO {
    private long totalUsers;
    private long activeUsers;
    private long blockedUsers;
    private long pendingCourseApprovals;
}
