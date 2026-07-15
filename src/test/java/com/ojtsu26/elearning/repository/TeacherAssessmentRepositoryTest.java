package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teacher_assessment_repository_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TeacherAssessmentRepositoryTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CodingAssignmentRepository codingAssignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    @Test
    void teacherAssignmentQueryFiltersByOwnerStatusSearchAndPaginates() {
        User teacherA = persistUser("teacher.a@example.com", Role.TEACHER);
        User teacherB = persistUser("teacher.b@example.com", Role.TEACHER);
        Course courseA = persistCourse("Java", teacherA);
        Course courseB = persistCourse("Python", teacherB);
        CodingAssignment javaOne = persistAssignment("Loops Lab", "PUBLISHED", courseA);
        persistAssignment("Arrays Lab", "DRAFT", courseA);
        persistAssignment("Other Teacher Lab", "PUBLISHED", courseB);
        entityManager.flush();
        entityManager.clear();

        Page<CodingAssignment> published = codingAssignmentRepository.findTeacherAssignments(
                teacherA.getId(), null, "PUBLISHED", "loops", PageRequest.of(0, 10));

        assertThat(published.getTotalElements()).isEqualTo(1);
        assertThat(published.getContent()).extracting(CodingAssignment::getId).containsExactly(javaOne.getId());

        Page<CodingAssignment> firstPage = codingAssignmentRepository.findTeacherAssignments(
                teacherA.getId(), null, null, null, PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);
    }

    @Test
    void teacherSubmissionQueryFiltersByOwnerAssignmentStatusAndStudentSearch() {
        User teacherA = persistUser("teacher.a.sub@example.com", Role.TEACHER);
        User teacherB = persistUser("teacher.b.sub@example.com", Role.TEACHER);
        User studentA = persistUser("student.a@example.com", Role.STUDENT);
        User studentB = persistUser("student.b@example.com", Role.STUDENT);
        Course courseA = persistCourse("Algorithms", teacherA);
        Course courseB = persistCourse("Databases", teacherB);
        CodingAssignment assignmentA = persistAssignment("Stacks Lab", "PUBLISHED", courseA);
        CodingAssignment assignmentB = persistAssignment("Indexes Lab", "PUBLISHED", courseB);
        Submission ownSubmission = persistSubmission(assignmentA, studentA, SubmissionStatus.SUBMITTED);
        persistSubmission(assignmentA, studentB, SubmissionStatus.GRADED);
        persistSubmission(assignmentB, studentA, SubmissionStatus.SUBMITTED);
        entityManager.flush();
        entityManager.clear();

        Page<Submission> submissions = submissionRepository.findTeacherSubmissions(
                teacherA.getId(),
                courseA.getId(),
                assignmentA.getId(),
                SubmissionStatus.SUBMITTED,
                "student.a",
                PageRequest.of(0, 10));

        assertThat(submissions.getTotalElements()).isEqualTo(1);
        assertThat(submissions.getContent()).extracting(Submission::getId).containsExactly(ownSubmission.getId());
    }

    private User persistUser(String email, Role role) {
        User user = User.builder()
                .fullName(email.substring(0, email.indexOf('@')))
                .email(email)
                .passwordHash("secret")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Course persistCourse(String title, User teacher) {
        Course course = Course.builder()
                .title(title)
                .description(title)
                .price(BigDecimal.ZERO)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build();
        entityManager.persist(course);
        return course;
    }

    private CodingAssignment persistAssignment(String title, String status, Course course) {
        Lesson lesson = Lesson.builder()
                .title(title)
                .type(LessonType.CODING)
                .course(course)
                .orderIndex(1)
                .build();
        entityManager.persist(lesson);
        CodingAssignment assignment = CodingAssignment.builder()
                .title(title)
                .status(status)
                .maxScore(new BigDecimal("100.00"))
                .lesson(lesson)
                .build();
        entityManager.persist(assignment);
        return assignment;
    }

    private Submission persistSubmission(CodingAssignment assignment, User student, SubmissionStatus status) {
        Submission submission = Submission.builder()
                .assignment(assignment)
                .lesson(assignment.getLesson())
                .student(student)
                .status(status)
                .submittedContent("answer")
                .build();
        entityManager.persist(submission);
        return submission;
    }
}
