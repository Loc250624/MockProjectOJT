package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherCourseStudentPageDTO {
    private Integer courseId;
    private String courseTitle;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private String search;
    private String enrollmentStatus;
    private String progressState;
    private String lastActivityFrom;
    private String lastActivityTo;
    private String sort;
    private String direction;
    private boolean emailVisible;
    private List<TeacherCourseStudentDTO> students;
}
