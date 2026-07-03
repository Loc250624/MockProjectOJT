package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherCourseStudentPageDTO;
import com.ojtsu26.elearning.dto.response.TeacherProgressOverviewDTO;
import com.ojtsu26.elearning.dto.response.TeacherStudentProgressDetailDTO;

import java.time.LocalDate;

public interface TeacherCourseStudentService {
    TeacherCourseStudentPageDTO findStudentsForCurrentTeacherCourse(Integer courseId,
                                                                    String search,
                                                                    String enrollmentStatus,
                                                                    String progressState,
                                                                    LocalDate lastActivityFrom,
                                                                    LocalDate lastActivityTo,
                                                                    String sort,
                                                                    String direction,
                                                                    Integer page,
                                                                    Integer size);

    TeacherProgressOverviewDTO getProgressOverviewForCurrentTeacherCourse(Integer courseId);

    TeacherStudentProgressDetailDTO getStudentProgressDetailForCurrentTeacherCourse(Integer courseId,
                                                                                    Integer studentId);
}
