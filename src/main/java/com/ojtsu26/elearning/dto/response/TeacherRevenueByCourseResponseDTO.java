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
public class TeacherRevenueByCourseResponseDTO {
    private LocalDate from;
    private LocalDate to;
    private Integer courseId;
    private List<TeacherRevenueCourseDTO> courses;
}
