package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherCourseStudentPageDTO;

public interface TeacherCourseStudentService {
    TeacherCourseStudentPageDTO findStudentsForCurrentTeacherCourse(Integer courseId,
                                                                    String search,
                                                                    String enrollmentStatus,
                                                                    String progressState,
                                                                    String sort,
                                                                    String direction,
                                                                    Integer page,
                                                                    Integer size);
}
