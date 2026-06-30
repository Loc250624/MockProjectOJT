package com.ojtsu26.elearning.seeder;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CoreDataSeeder {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RoadmapRepository roadmapRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final VideoRepository videoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        seedUsers();
        seedCategories();
        seedRoadmaps();
        seedCourses();
        seedLessonsAndVideos();

        // Fix existing courses thumbnails and ensure category coverage for QA
        List<Course> allCourses = courseRepository.findAll();
        boolean updatedAny = false;
        for (Course c : allCourses) {
            if (c.getThumbnailUrl() == null || c.getThumbnailUrl().contains("example.com")) {
                c.setThumbnailUrl(getThumbnailForCategory(c.getCategory() != null ? c.getCategory().getName() : null));
                updatedAny = true;
            }
        }

        List<Category> categories = categoryRepository.findAll();
        for (Category cat : categories) {
            List<Course> catCourses = allCourses.stream()
                    .filter(c -> c.getCategory() != null && c.getCategory().getId().equals(cat.getId()))
                    .collect(Collectors.toList());
            if (!catCourses.isEmpty()) {
                boolean hasApproved = catCourses.stream().anyMatch(c -> c.getStatus() == CourseStatus.APPROVED);
                if (!hasApproved) {
                    catCourses.get(0).setStatus(CourseStatus.APPROVED);
                    updatedAny = true;
                }
            }
        }

        if (updatedAny) {
            courseRepository.saveAll(allCourses);
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;
        
        String password = passwordEncoder.encode("123456");
        
        // 3 Admins
        for (int i = 0; i < 3; i++) {
            userRepository.save(createUser("admin" + i + "@elearning.com", Role.ADMIN, password));
        }
        
        // 5 Teachers
        for (int i = 0; i < 5; i++) {
            userRepository.save(createUser("teacher" + i + "@elearning.com", Role.TEACHER, password));
        }
        
        // 35 Students
        for (int i = 0; i < 35; i++) {
            userRepository.save(createUser("student" + i + "@elearning.com", Role.STUDENT, password));
        }
    }
    
    private User createUser(String email, Role role, String password) {
        String name = SeederUtils.getRandomFirstName() + " " + SeederUtils.getRandomLastName();
        return User.builder()
                .email(email)
                .fullName(name)
                .passwordHash(password)
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .createdAt(SeederUtils.getRandomPastDate(300))
                .build();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        for (int i = 0; i < 15; i++) {
            Category category = Category.builder()
                    .name(SeederUtils.CATEGORIES[i % SeederUtils.CATEGORIES.length] + (i > SeederUtils.CATEGORIES.length - 1 ? " " + i : ""))
                    .description(SeederUtils.getRandomDescription())
                    .build();
            categoryRepository.save(category);
        }
    }

    private void seedRoadmaps() {
        if (roadmapRepository.count() > 0) return;
        List<User> teachers = userRepository.findAll().stream().filter(u -> u.getRole() == Role.TEACHER).collect(Collectors.toList());
        if (teachers.isEmpty()) return;

        for (int i = 0; i < 20; i++) {
            Roadmap roadmap = Roadmap.builder()
                    .title(SeederUtils.getRandomRoadmapTitle() + " " + (i + 1))
                    .description(SeederUtils.getRandomDescription())
                    .instructor(SeederUtils.getRandomElement(teachers))
                    .createdAt(SeederUtils.getRandomPastDate(200))
                    .build();
            roadmapRepository.save(roadmap);
        }
    }

    private void seedCourses() {
        if (courseRepository.count() > 0) return;
        List<User> teachers = userRepository.findAll().stream().filter(u -> u.getRole() == Role.TEACHER).collect(Collectors.toList());
        List<Category> categories = categoryRepository.findAll();
        List<Roadmap> roadmaps = roadmapRepository.findAll();
        
        if (teachers.isEmpty() || categories.isEmpty()) return;

        for (int i = 0; i < 50; i++) {
            Category category = categories.get(i % categories.size());
            // 60% APPROVED, 20% DRAFT, 10% PENDING_APPROVAL, 10% HIDDEN
            CourseStatus status;
            if (i % 10 < 6) {
                status = CourseStatus.APPROVED;
            } else if (i % 10 < 8) {
                status = CourseStatus.DRAFT;
            } else if (i % 10 == 8) {
                status = CourseStatus.PENDING_APPROVAL;
            } else {
                status = CourseStatus.HIDDEN;
            }

            Course course = Course.builder()
                    .title(SeederUtils.getRandomCourseTitle() + " " + (i + 1))
                    .description(SeederUtils.getRandomDescription())
                    .price(new BigDecimal(SeederUtils.getRandomInt(10, 200)))
                    .status(status)
                    .instructor(SeederUtils.getRandomElement(teachers))
                    .category(category)
                    .roadmap(SeederUtils.getRandomElement(roadmaps))
                    .thumbnailUrl(getThumbnailForCategory(category.getName()))
                    .createdAt(SeederUtils.getRandomPastDate(150))
                    .build();
            courseRepository.save(course);
        }
    }

    private String getThumbnailForCategory(String categoryName) {
        if (categoryName == null) return "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500&auto=format&fit=crop";
        switch (categoryName) {
            case "Java":
                return "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500&auto=format&fit=crop";
            case "Spring Boot":
                return "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500&auto=format&fit=crop";
            case "Web Development":
                return "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=500&auto=format&fit=crop";
            case "Frontend":
                return "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500&auto=format&fit=crop";
            case "Backend":
                return "https://images.unsplash.com/photo-1605379399642-870262d3d051?w=500&auto=format&fit=crop";
            case "Database":
                return "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=500&auto=format&fit=crop";
            case "DevOps":
                return "https://images.unsplash.com/photo-1618401471353-b98aedd07871?w=500&auto=format&fit=crop";
            case "Artificial Intelligence":
                return "https://images.unsplash.com/photo-1677442136019-21780efad99a?w=500&auto=format&fit=crop";
            case "Mobile Development":
                return "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=500&auto=format&fit=crop";
            case "UI/UX":
                return "https://images.unsplash.com/photo-1586717791821-3f44a563fa4c?w=500&auto=format&fit=crop";
            case "Data Science":
                return "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=500&auto=format&fit=crop";
            case "Cloud Computing":
                return "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=500&auto=format&fit=crop";
            case "Cybersecurity":
                return "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=500&auto=format&fit=crop";
            case "Game Development":
                return "https://images.unsplash.com/photo-1556438064-2d7646166914?w=500&auto=format&fit=crop";
            case "Blockchain":
                return "https://images.unsplash.com/photo-1621761191319-c6fb62004040?w=500&auto=format&fit=crop";
            default:
                return "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500&auto=format&fit=crop";
        }
    }

    private void seedLessonsAndVideos() {
        if (lessonRepository.count() > 0) return;
        List<Course> courses = courseRepository.findAll();
        if (courses.isEmpty()) return;

        int lessonCount = 0;
        int targetLessons = 400; // Between 300-500
        int videoCount = 0;
        int targetVideos = 350; // Between 250-400

        LessonType[] types = {LessonType.VIDEO, LessonType.QUIZ, LessonType.CODING};

        for (Course course : courses) {
            int lessonsForCourse = SeederUtils.getRandomInt(5, 12);
            for (int i = 0; i < lessonsForCourse; i++) {
                if (lessonCount >= targetLessons) break;

                LessonType type = types[i % 3]; // Ensuring a mix
                
                // Bias towards Video to meet video targets
                if (videoCount < targetVideos && SeederUtils.getRandomInt(1, 10) <= 7) {
                    type = LessonType.VIDEO;
                }
                
                Lesson lesson = Lesson.builder()
                        .title(SeederUtils.getRandomLessonTitle() + " " + (i + 1))
                        .content("Lesson content goes here...")
                        .type(type)
                        .orderIndex(i + 1)
                        .course(course)
                        .createdAt(SeederUtils.getRandomPastDate(100))
                        .build();
                
                lesson = lessonRepository.save(lesson);
                lessonCount++;

                if (type == LessonType.VIDEO && videoCount < targetVideos) {
                    String[] sampleVideos = {
                        "https://www.w3schools.com/html/mov_bbb.mp4",
                        "https://media.w3.org/2010/05/sintel/trailer.mp4",
                        "https://media.w3.org/2010/05/bunny/trailer.mp4",
                        "https://media.w3.org/2010/05/video/movie_300.mp4",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/friday.mp4"
                    };
                    Video video = Video.builder()
                            .videoUrl(sampleVideos[videoCount % sampleVideos.length])
                            .durationSeconds(SeederUtils.getRandomInt(120, 3600))
                            .lesson(lesson)
                            .build();
                    videoRepository.save(video);
                    videoCount++;
                }
            }
        }
    }
}
