package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogCleanupFrontendTemplateTest {

    @Test
    void blogSearchIsDebouncedLiveSearchWithoutSubmitButton() throws Exception {
        String template = read("src/main/resources/templates/public/blogs.html");
        String script = read("src/main/resources/static/js/live-search.js");

        assertTrue(template.contains("data-live-search-target=\"#blog-live-results\""));
        assertTrue(template.contains("aria-label=\"Search published blogs\""));
        assertTrue(template.contains("th:src=\"@{/js/live-search.js}\""));
        assertFalse(template.contains(">Search</button>"));
        assertFalse(template.contains(">Search</span>"));
        assertFalse(template.contains("Search Search"));

        assertTrue(script.contains("var DEBOUNCE_MS = 300;"));
        assertTrue(script.contains("new AbortController()"));
        assertTrue(script.contains("requestId !== latestRequestId"));
        assertTrue(script.contains("params.delete('page')"));
        assertTrue(script.contains("input.addEventListener('input', scheduleSearch)"));
    }

    @Test
    void matchingCourseCatalogSearchBarsAlsoUseLiveSearchWithoutButtons() throws Exception {
        String publicCourses = read("src/main/resources/templates/public/courses.html");
        String studentCourses = read("src/main/resources/templates/student/courses.html");

        assertTrue(publicCourses.contains("data-live-search-target=\"#public-course-live-results\""));
        assertTrue(publicCourses.contains("aria-label=\"Search courses, instructors, and topics\""));
        assertFalse(publicCourses.contains(">Search</button>"));

        assertTrue(studentCourses.contains("data-live-search-target=\"#student-course-live-results\""));
        assertTrue(studentCourses.contains("aria-label=\"Search courses\""));
        assertFalse(studentCourses.contains("<span>Search</span>"));
    }

    @Test
    void blogSchemaIsVerifiedAgainstRuntimeResourcesWithoutRootDataSql() throws Exception {
        String applicationProperties = read("src/main/resources/application.properties");
        String pom = read("pom.xml");
        String blogPost = read("src/main/java/com/ojtsu26/elearning/model/entity/BlogPost.java");
        String blogComment = read("src/main/java/com/ojtsu26/elearning/model/entity/BlogComment.java");
        List<Path> sqlResources;
        try (var paths = Files.walk(Path.of("src/main/resources"))) {
            sqlResources = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sql"))
                    .toList();
        }

        assertFalse(Files.exists(Path.of("data.sql")),
                "Runtime SQL resources must not be coupled to a working-directory root data.sql");
        assertFalse(applicationProperties.contains("spring.sql.init."),
                "The current app does not enable Spring SQL init for Blog seed cleanup");
        assertFalse(pom.toLowerCase().contains("flyway"));
        assertFalse(pom.toLowerCase().contains("liquibase"));

        assertTrue(blogPost.contains("@Table(name = \"BlogPosts\")"));
        assertTrue(blogPost.contains("private String content;"));
        assertTrue(blogPost.contains("@Column(nullable = false)"));
        assertTrue(blogPost.contains("private boolean deleted = false;"));
        assertTrue(blogComment.contains("@Table(name = \"BlogComments\")"));
        assertTrue(blogComment.contains("@JoinColumn(name = \"blog_post_id\", nullable = false)"));

        assertFalse(sqlResources.isEmpty());
        for (Path sqlResource : sqlResources) {
            String sql = Files.readString(sqlResource);
            assertFalse(sql.contains("LOCK TABLES `blog_posts` WRITE;"),
                    "Obsolete dump-style blog seed should not be verified as runtime data: " + sqlResource);
            assertFalse(sql.matches("(?is).*\\bTRUNCATE\\s+(?:TABLE\\s+)?`?blog_posts`?.*"),
                    "Blog cleanup resources must not truncate BlogPosts: " + sqlResource);
        }
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
