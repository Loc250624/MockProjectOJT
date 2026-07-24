package com.ojtsu26.elearning.seeder;

import com.ojtsu26.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final CoreDataSeeder coreDataSeeder;
    private final LearningDataSeeder learningDataSeeder;
    private final BusinessDataSeeder businessDataSeeder;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RoadmapRepository roadmapRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final VideoRepository videoRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CodingAssignmentRepository assignmentRepository;
    private final TestcaseRepository testcaseRepository;
    private final SubmissionRepository submissionRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateRepository certificateRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting Database Seeding...");
        
        coreDataSeeder.seed();
        learningDataSeeder.seed();
        businessDataSeeder.seed();

        System.out.println("Users inserted: " + userRepository.count());
        System.out.println("Categories inserted: " + categoryRepository.count());
        System.out.println("Roadmaps inserted: " + roadmapRepository.count());
        System.out.println("Courses inserted: " + courseRepository.count());
        System.out.println("Lessons inserted: " + lessonRepository.count());
        System.out.println("Videos inserted: " + videoRepository.count());
        System.out.println("Enrollments inserted: " + enrollmentRepository.count());
        System.out.println("Lesson Progress inserted: " + lessonProgressRepository.count());
        System.out.println("Quizzes inserted: " + quizRepository.count());
        System.out.println("Questions inserted: " + questionRepository.count());
        System.out.println("Assignments inserted: " + assignmentRepository.count());
        System.out.println("Testcases inserted: " + testcaseRepository.count());
        System.out.println("Submissions inserted: " + submissionRepository.count());
        System.out.println("Transactions inserted: " + transactionRepository.count());
        System.out.println("Certificates inserted: " + certificateRepository.count());
        System.out.println();
        System.out.println("Demo data generated successfully.");
    }
}
