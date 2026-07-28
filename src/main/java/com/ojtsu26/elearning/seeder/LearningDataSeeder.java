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
    private final SubmissionRepository submissionRepository;
    
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public void seed() {
        seedQuizzesAndQuestions();
        seedCourseEnrollments();
        seedLessonProgress();
        seedSubmissions();
    }

    private void seedQuizzesAndQuestions() {
        if (quizRepository.count() > 0) return;
        List<Lesson> quizLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getType() == LessonType.QUIZ).collect(Collectors.toList());
        
        int quizCount = 0;
        int questionCount = 0;
        
        for (Lesson lesson : quizLessons) {
            Quiz quiz = Quiz.builder()
                    .lesson(lesson)
                    .title("Quiz: " + lesson.getTitle())
                    .description("Auto-generated assessment quiz for " + lesson.getTitle())
                    .durationMinutes(30)
                    .maxAttempts(2)
                    .status(QuizStatus.PUBLISHED)
                    .createdBy(lesson.getCourse() == null ? null : lesson.getCourse().getInstructor())
                    .passingScore(new BigDecimal("70.00"))
                    .build();
            quiz = quizRepository.save(quiz);
            quizCount++;
            
            int questionsForQuiz = SeederUtils.getRandomInt(5, 10);
            for (int i = 0; i < questionsForQuiz; i++) {
                Question question = Question.builder()
                        .quiz(quiz)
                        .questionText("Question " + (i + 1) + " for " + lesson.getTitle())
                        .optionsJson("[{\"content\":\"A\",\"correct\":true},{\"content\":\"B\",\"correct\":false},{\"content\":\"C\",\"correct\":false},{\"content\":\"D\",\"correct\":false}]")
                        .correctAnswer("0")
                        .questionType(QuestionType.SINGLE_CHOICE)
                        .points(BigDecimal.ONE)
                        .displayOrder(i + 1)
                        .build();
                questionRepository.save(question);
                questionCount++;
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
                .filter(l -> l.getType() != LessonType.RETIRED)
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
                .filter(l -> l.getType() == LessonType.QUIZ)
                .collect(Collectors.toList());
                
            for (Lesson lesson : assignableLessons) {
                if (submissionCount >= targetSubmissions) break;
                if (SeederUtils.getRandomBoolean()) { // 50% chance they submitted it
                    Submission submission = Submission.builder()
                            .student(enrollment.getStudent())
                            .lesson(lesson)
                            .score(new BigDecimal(SeederUtils.getRandomInt(0, 100)))
                            .status(SeederUtils.getRandomElement(statuses))
                            .submittedContent("{\"type\":\"QUIZ\",\"state\":\"SUBMITTED\",\"answers\":{}}")
                            .submittedAt(SeederUtils.getRandomPastDate(15))
                            .build();
                    submissionRepository.save(submission);
                    submissionCount++;
                }
            }
        }
    }
}
