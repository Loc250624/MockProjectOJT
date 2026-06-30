package com.ojtsu26.elearning.seeder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class SeederUtils {
    private static final Random random = new Random();

    public static final String[] FIRST_NAMES = {"James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen"};
    public static final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin"};
    public static final String[] COURSE_TITLES = {"Java Core Masterclass", "Spring Boot for Beginners", "Spring Boot Advanced", "REST API Development", "Hibernate Deep Dive", "React Fundamentals", "Docker Essentials", "Kubernetes for Developers", "MySQL Complete Guide", "Machine Learning Basics", "Python for Data Science", "Advanced Angular", "Microservices with Spring Cloud", "AWS Certified Developer", "Azure Fundamentals", "Node.js Complete Guide", "Vue.js Masterclass", "C# and .NET Core", "Golang for Beginners", "Rust Programming"};
    public static final String[] LESSON_TITLES = {"Introduction", "Environment Setup", "Basic Concepts", "Deep Dive", "Advanced Topics", "Hands-on Practice", "Mini Project", "Common Pitfalls", "Best Practices", "Summary", "Conclusion", "Next Steps"};
    public static final String[] ROADMAP_TITLES = {"Java Backend Developer", "Spring Boot Professional", "Full Stack Web Developer", "AI Engineer", "Database Engineer", "Frontend Specialist", "DevOps Master", "Cloud Architect", "Mobile Developer iOS", "Mobile Developer Android"};
    public static final String[] BLOG_TITLES = {"How to learn Java fast", "Top 10 Spring Boot Features", "Why React is so popular", "Docker vs Kubernetes", "Understanding Hibernate Caching", "Getting started with Machine Learning", "The Future of Web Development", "Best IDEs for 2026", "Mastering REST APIs", "SQL vs NoSQL"};
    public static final String[] CATEGORIES = {"Java", "Spring Boot", "Web Development", "Frontend", "Backend", "Database", "DevOps", "Artificial Intelligence", "Mobile Development", "UI/UX", "Data Science", "Cloud Computing", "Cybersecurity", "Game Development", "Blockchain"};
    public static final String[] DESCRIPTIONS = {"This is a comprehensive guide to master the topic.", "Learn the fundamentals and advanced concepts.", "Step-by-step tutorial for beginners.", "Build real-world projects and enhance your skills.", "A deep dive into the core architecture.", "Best practices and industry standards explained.", "Hands-on exercises and practical examples.", "Unlock your potential with this complete course."};
    
    public static String getRandomFirstName() { return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]; }
    public static String getRandomLastName() { return LAST_NAMES[random.nextInt(LAST_NAMES.length)]; }
    public static String getRandomCourseTitle() { return COURSE_TITLES[random.nextInt(COURSE_TITLES.length)]; }
    public static String getRandomLessonTitle() { return LESSON_TITLES[random.nextInt(LESSON_TITLES.length)]; }
    public static String getRandomRoadmapTitle() { return ROADMAP_TITLES[random.nextInt(ROADMAP_TITLES.length)]; }
    public static String getRandomBlogTitle() { return BLOG_TITLES[random.nextInt(BLOG_TITLES.length)]; }
    public static String getRandomCategory() { return CATEGORIES[random.nextInt(CATEGORIES.length)]; }
    public static String getRandomDescription() { return DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)]; }

    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }
    
    public static <T> T getRandomElement(T[] array) {
        if (array == null || array.length == 0) return null;
        return array[random.nextInt(array.length)];
    }

    public static LocalDateTime getRandomPastDate(int daysBack) {
        return LocalDateTime.now().minusDays(random.nextInt(daysBack + 1)).minusHours(random.nextInt(24)).minusMinutes(random.nextInt(60));
    }

    public static int getRandomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }
    
    public static boolean getRandomBoolean() {
        return random.nextBoolean();
    }
}
