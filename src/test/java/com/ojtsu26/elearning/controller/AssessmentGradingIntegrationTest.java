package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.GradeFeedbackRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:assessment_grading_integration_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AssessmentGradingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private CodingAssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private GradeFeedbackRepository gradeFeedbackRepository;

    @Test
    void studentSubmitTeacherDraftPublishThenStudentSeesReleasedResult() throws Exception {
        long suffix = System.nanoTime();
        User teacher = persistUser("Teacher Owner", "teacher.owner." + suffix + "@example.com", Role.TEACHER);
        User otherTeacher = persistUser("Teacher Other", "teacher.other." + suffix + "@example.com", Role.TEACHER);
        User student = persistUser("Student One", "student.one." + suffix + "@example.com", Role.STUDENT);
        Course course = courseRepository.save(Course.builder()
                .title("Assessment Course")
                .description("Course")
                .price(BigDecimal.ZERO)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        enrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(course)
                .isCompleted(false)
                .progressPercentage(BigDecimal.ZERO)
                .build());
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("Loops Assignment")
                .type(LessonType.CODING)
                .orderIndex(1)
                .course(course)
                .build());
        CodingAssignment assignment = assignmentRepository.save(CodingAssignment.builder()
                .title("Loops Lab")
                .problemStatement("Solve loops")
                .allowedLanguages("Java")
                .maxScore(new BigDecimal("100.00"))
                .status("PUBLISHED")
                .lesson(lesson)
                .build());

        mockMvc.perform(post("/api/student/assignments/{assignmentId}/submissions/submit", assignment.getId())
                        .with(user(principal(student)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentText\":\"student solution\",\"codeLanguage\":\"Java\",\"codeContent\":\"class Solution {}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        Submission submission = submissionRepository
                .findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(assignment.getId(), student.getId())
                .orElseThrow();

        mockMvc.perform(get("/teacher/grading")
                        .with(user(principal(teacher)))
                        .param("assignmentId", assignment.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Student One")))
                .andExpect(content().string(containsString("Loops Lab")));

        mockMvc.perform(post("/api/teacher/submissions/{submissionId}/grade", submission.getId())
                        .with(user(principal(otherTeacher)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":84.50,\"feedback\":\"Other feedback\",\"publish\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/teacher/submissions/{submissionId}/grade", submission.getId())
                        .with(user(principal(teacher)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":84.50,\"feedback\":\"Draft feedback\",\"publish\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.released").value(false));

        Submission draft = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(draft.getStatus()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(gradeFeedbackRepository.findTopBySubmissionIdOrderByGradedAtDesc(submission.getId())).isPresent();

        mockMvc.perform(get("/student/submissions/{submissionId}/result", submission.getId())
                        .with(user(principal(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("awaiting final published feedback")))
                .andExpect(content().string(not(containsString("Draft feedback"))))
                .andExpect(content().string(not(containsString("84.50%"))));

        mockMvc.perform(post("/api/teacher/submissions/{submissionId}/grade", submission.getId())
                        .with(user(principal(teacher)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":84.50,\"feedback\":\"Final feedback\",\"publish\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADED"))
                .andExpect(jsonPath("$.data.released").value(true));

        Submission published = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(SubmissionStatus.GRADED);

        mockMvc.perform(get("/student/submissions/{submissionId}/result", submission.getId())
                        .with(user(principal(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("84.50%")))
                .andExpect(content().string(containsString("Final feedback")));
    }

    private User persistUser(String name, String email, Role role) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .passwordHash("secret")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }

    private CustomUserDetails principal(User user) {
        return new CustomUserDetails(user);
    }
}
