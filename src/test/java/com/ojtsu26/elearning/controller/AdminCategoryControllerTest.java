package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CategoryRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_categories_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User teacher;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        teacher = userRepository.save(User.builder()
                .fullName("Category Teacher")
                .email("category.teacher@example.com")
                .passwordHash("secret")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editCategoryMetadataSucceedsEvenWhenCoursesUseIt() throws Exception {
        Category category = categoryRepository.save(category(" Backend ", " Old description "));
        Course course = courseRepository.save(course("Spring Course", category));

        mockMvc.perform(patch("/api/admin/categories/{id}", category.getId())
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\" Backend Development \",\"description\":\" Updated description \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Backend Development"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.courseCount").value(1));

        Course persistedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(persistedCourse.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateCategoryNameReturnsConflict() throws Exception {
        categoryRepository.save(category("Data Science", "One"));
        Category category = categoryRepository.save(category("AI", "Two"));

        mockMvc.perform(patch("/api/admin/categories/{id}", category.getId())
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\" data science \",\"description\":\"Duplicate\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category name already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUnusedCategoryRemovesOnlyCategory() throws Exception {
        Category category = categoryRepository.save(category("Unused", "Safe to delete"));

        mockMvc.perform(delete("/api/admin/categories/{id}", category.getId()).with(csrf()))
                .andExpect(status().isOk());

        assertThat(categoryRepository.existsById(category.getId())).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUsedCategoryReturnsStructuredConflictAndKeepsCourse() throws Exception {
        Category category = categoryRepository.save(category("Used", "Has courses"));
        Course course = courseRepository.save(course("Used Course", category));

        mockMvc.perform(delete("/api/admin/categories/{id}", category.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.categoryId").value(category.getId()))
                .andExpect(jsonPath("$.data.dependentCourseCount").value(1))
                .andExpect(content().string(containsString("Reassign the courses before deleting")));

        assertThat(categoryRepository.existsById(category.getId())).isTrue();
        assertThat(courseRepository.existsById(course.getId())).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reassignCoursesAndDeleteMovesCoursesTransactionally() throws Exception {
        Category source = categoryRepository.save(category("Source", "Delete me"));
        Category replacement = categoryRepository.save(category("Replacement", "Keep me"));
        Course course = courseRepository.save(course("Moved Course", source));

        mockMvc.perform(post("/api/admin/categories/{id}/reassign-and-delete", source.getId())
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"replacementCategoryId\":" + replacement.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dependentCourseCount").value(1));

        assertThat(categoryRepository.existsById(source.getId())).isFalse();
        assertThat(courseRepository.findById(course.getId()).orElseThrow().getCategory().getId())
                .isEqualTo(replacement.getId());
    }

    private Category category(String name, String description) {
        return Category.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Course course(String title, Category category) {
        return Course.builder()
                .title(title)
                .description("Course description")
                .price(BigDecimal.TEN)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .category(category)
                .build();
    }
}
