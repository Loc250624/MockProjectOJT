package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void seedAndMigrationAreConfiguredForTheVerifiedBlogSchema() throws Exception {
        String seed = read("data.sql");
        int sectionStart = seed.indexOf("LOCK TABLES `blog_posts` WRITE;");
        int sectionEnd = seed.indexOf("UNLOCK TABLES;", sectionStart);
        assertTrue(sectionStart >= 0 && sectionEnd > sectionStart);

        String blogSeed = seed.substring(sectionStart, sectionEnd).toLowerCase();
        assertFalse(blogSeed.matches("(?s).*</?(h[1-6]|p|code)(\\s[^>]*)?>.*"));

        String safeUpdate = read("lumina_blog_cleanup_live_search_codex/sql/01_safe_update_strip_blog_html.sql");
        String deleteInsert = read("lumina_blog_cleanup_live_search_codex/sql/02_delete_and_reinsert_without_html_tags.sql");
        assertTrue(safeUpdate.contains("SET @blog_table = 'blog_posts';"));
        assertTrue(safeUpdate.contains("SET @blog_content_column = 'content';"));
        assertTrue(deleteInsert.contains("'DELETE FROM `', @blog_table, '`'"));
        assertTrue(deleteInsert.contains("'INSERT INTO `', @blog_table"));
        assertFalse(deleteInsert.matches("(?is).*\\bTRUNCATE\\s+(?:TABLE\\s+)?`?blog_posts`?.*"));
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
