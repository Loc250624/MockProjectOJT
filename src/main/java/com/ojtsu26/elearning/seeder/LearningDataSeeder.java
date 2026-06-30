package com.ojtsu26.elearning.seeder;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LearningDataSeeder {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CodingAssignmentRepository codingAssignmentRepository;
    private final TestcaseRepository testcaseRepository;
    private final SubmissionRepository submissionRepository;
    
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public void seed() {
        seedQuizzesAndQuestions();
        seedCodingAssignmentsAndTestcases();
        seedCourseEnrollments();
        seedLessonProgress();
        seedSubmissions();
    }

    private void seedQuizzesAndQuestions() {
        if (quizRepository.count() > 0) return;
        List<Lesson> quizLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getType() == LessonType.QUIZ).collect(Collectors.toList());
        
        int quizCount = 0;
        int targetQuizzes = 50; // between 40-60
        int questionCount = 0;
        
        for (Lesson lesson : quizLessons) {
            if (quizCount >= targetQuizzes) break;
            
            Quiz quiz = Quiz.builder()
                    .lesson(lesson)
                    .title("Quiz: " + lesson.getTitle())
                    .passingScore(new BigDecimal("70.00"))
                    .build();
            quiz = quizRepository.save(quiz);
            quizCount++;
            
            int questionsForQuiz = SeederUtils.getRandomInt(5, 10);
            for (int i = 0; i < questionsForQuiz; i++) {
                Question question = Question.builder()
                        .quiz(quiz)
                        .questionText("Question " + (i + 1) + " for " + lesson.getTitle())
                        .optionsJson("[\"A\", \"B\", \"C\", \"D\"]")
                        .correctAnswer("A")
                        .build();
                questionRepository.save(question);
                questionCount++;
            }
        }
    }

    private void seedCodingAssignmentsAndTestcases() {
        if (codingAssignmentRepository.count() > 0) return;
        List<Lesson> codingLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getType() == LessonType.CODING).collect(Collectors.toList());

        int assignmentCount = 0;
        int targetAssignments = 25; // between 20-30
        
        for (Lesson lesson : codingLessons) {
            if (assignmentCount >= targetAssignments) break;
            
            CodingAssignment assignment = CodingAssignment.builder()
                    .lesson(lesson)
                    .title("Coding: " + lesson.getTitle())
                    .problemStatement("Solve the following problem using Java.")
                    .allowedLanguages("Java, Python")
                    .timeLimitMs(2000)
                    .build();
            assignment = codingAssignmentRepository.save(assignment);
            assignmentCount++;
            
            int testcasesForAssignment = SeederUtils.getRandomInt(3, 8);
            for (int i = 0; i < testcasesForAssignment; i++) {
                Testcase testcase = Testcase.builder()
                        .assignment(assignment)
                        .inputData("input " + (i + 1))
                        .expectedOutput("output " + (i + 1))
                        .isHidden(SeederUtils.getRandomBoolean())
                        .build();
                testcaseRepository.save(testcase);
            }
        }
    }

    private void seedCourseEnrollments() {
        if (enrollmentRepository.count() > 0) return;
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT).collect(Collectors.toList());
        List<Course> approvedCourses = courseRepository.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.APPROVED).collect(Collectors.toList());

        if (students.isEmpty() || approvedCourses.isEmpty()) return;

        int enrollmentCount = 0;
        int targetEnrollments = 220; // between 150-300

        for (int i = 0; i < targetEnrollments; i++) {
            User student = SeederUtils.getRandomElement(students);
            Course course = SeederUtils.getRandomElement(approvedCourses);
            
            boolean exists = enrollmentRepository.findAll().stream()
                .anyMatch(e -> e.getStudent().getId().equals(student.getId()) && e.getCourse().getId().equals(course.getId()));
            
            if (exists) continue; // avoid duplicates

            BigDecimal progress = new BigDecimal(SeederUtils.getRandomInt(0, 100));
            boolean isCompleted = progress.compareTo(new BigDecimal(100)) == 0;

            CourseEnrollment enrollment = CourseEnrollment.builder()
                    .student(student)
                    .course(course)
                    .progressPercentage(progress)
                    .isCompleted(isCompleted)
                    .enrolledAt(SeederUtils.getRandomPastDate(60))
                    .build();
            enrollmentRepository.save(enrollment);
            enrollmentCount++;
        }
    }

    private void seedLessonProgress() {
        if (lessonProgressRepository.count() > 0) return;
        List<CourseEnrollment> enrollments = enrollmentRepository.findAll();
        
        int targetProgress = 2000;
        int progressCount = 0;

        for (CourseEnrollment enrollment : enrollments) {
            List<Lesson> courseLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getCourse().getId().equals(enrollment.getCourse().getId()))
                .collect(Collectors.toList());
            
            int lessonsToComplete = (int) (courseLessons.size() * enrollment.getProgressPercentage().doubleValue() / 100);
            
            for (int i = 0; i < lessonsToComplete; i++) {
                if(progressCount >= targetProgress) break;
                Lesson lesson = courseLessons.get(i);
                
                LessonProgress progress = LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .isCompleted(true)
                        .completedAt(SeederUtils.getRandomPastDate(30))
                        .build();
                lessonProgressRepository.save(progress);
                progressCount++;
            }
        }
    }

    private void seedSubmissions() {
        if (submissionRepository.count() > 0) return;
        List<CourseEnrollment> enrollments = enrollmentRepository.findAll();
        
        SubmissionStatus[] statuses = SubmissionStatus.values();
        int submissionCount = 0;
        int targetSubmissions = 250;

        for (CourseEnrollment enrollment : enrollments) {
            if(submissionCount >= targetSubmissions) break;
            
            List<Lesson> assignableLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getCourse().getId().equals(enrollment.getCourse().getId()))
                .filter(l -> l.getType() == LessonType.QUIZ || l.getType() == LessonType.CODING)
                .collect(Collectors.toList());
                
            for (Lesson lesson : assignableLessons) {
                if (submissionCount >= targetSubmissions) break;
                if (SeederUtils.getRandomBoolean()) { // 50% chance they submitted it
                    Submission submission = Submission.builder()
                            .student(enrollment.getStudent())
                            .lesson(lesson)
                            .score(new BigDecimal(SeederUtils.getRandomInt(0, 100)))
                            .status(SeederUtils.getRandomElement(statuses))
                            .submittedContent("My solution code or quiz answers")
                            .teacherFeedback("Good job!")
                            .submittedAt(SeederUtils.getRandomPastDate(15))
                            .build();
                    submissionRepository.save(submission);
                    submissionCount++;
                }
            }
        }
    }
}
