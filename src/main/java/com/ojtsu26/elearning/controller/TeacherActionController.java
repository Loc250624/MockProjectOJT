package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.BlogPostRequestDTO;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.service.BlogPostService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.LessonService;
import com.ojtsu26.elearning.service.RoadmapService;
import com.ojtsu26.elearning.service.UserService;
import com.ojtsu26.elearning.service.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Objects;
import java.util.List;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherActionController {

    private final RoadmapService roadmapService;
    private final CourseService courseService;
    private final LessonService lessonService;
    private final VideoService videoService;
    private final UserService userService;
    private final JwtCookieService jwtCookieService;
    private final BlogPostService blogPostService;

    @PostMapping("/courses/create")
    public String createCourse(@Valid @ModelAttribute("course") CourseRequestDTO requestDTO, 
                               BindingResult bindingResult, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        if (userDetails != null && userDetails.getUser() != null) {
            requestDTO.setInstructorId(userDetails.getUser().getId());
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.course", bindingResult);
            redirectAttributes.addFlashAttribute("course", requestDTO);
            return "redirect:/teacher/courses/create";
        }
        try {
            courseService.create(requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Course created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("course", requestDTO);
            return "redirect:/teacher/courses/create";
        }
        return "redirect:/teacher/courses";
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequestDTO request,
            HttpServletResponse response) {
        String previousEmail = currentUser.getUsername();
        AuthProvider authProvider = currentUser.getUser().getAuthProvider();
        UserResponseDTO updatedProfile = userService.updateCurrentProfile(currentUser.getUser().getId(), request);

        if (authProvider == AuthProvider.LOCAL && !Objects.equals(previousEmail, updatedProfile.getEmail())) {
            jwtCookieService.addJwtCookie(response, updatedProfile.getEmail());
        }

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse() {
        return ResponseEntity.ok(Map.of("message", "Create course placeholder - not yet implemented"));
    }

    @PostMapping("/courses/edit/{id}")
    public String updateCourse(@PathVariable Integer id,
                               @Valid @ModelAttribute("course") CourseRequestDTO requestDTO, 
                               BindingResult bindingResult, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        if (userDetails != null && userDetails.getUser() != null) {
            requestDTO.setInstructorId(userDetails.getUser().getId());
        }
        
        CourseResponseDTO existing = courseService.findById(id);
        if (userDetails == null || userDetails.getUser() == null || !existing.getInstructorId().equals(userDetails.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You do not have permission to edit this course.");
            return "redirect:/teacher/courses";
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.course", bindingResult);
            redirectAttributes.addFlashAttribute("course", requestDTO);
            return "redirect:/teacher/courses/edit/" + id;
        }
        try {
            courseService.update(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Course updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("course", requestDTO);
            return "redirect:/teacher/courses/edit/" + id;
        }
        return "redirect:/teacher/courses";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Integer id, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            CourseResponseDTO existing = courseService.findById(id);
            if (userDetails == null || userDetails.getUser() == null || !existing.getInstructorId().equals(userDetails.getUser().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You do not have permission to delete this course.");
                return "redirect:/teacher/courses";
            }
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/courses";
    }

    // ----------------------------------------------------------------
    // CRS-08: Course Publishing Workflow
    // ----------------------------------------------------------------

    @PostMapping("/courses/submit/{id}")
    public String submitCourse(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;
        try {
            courseService.submitForReview(id, instructorId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Your course has been submitted for review. You will be notified once an admin reviews it.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/courses";
    }

    @PostMapping("/courses/withdraw/{id}")
    public String withdrawCourse(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;
        try {
            courseService.withdrawSubmission(id, instructorId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Your course has been withdrawn from review and is now back in Draft.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/courses";
    }

    @PostMapping("/roadmap/create")
    public String createRoadmap(@Valid @ModelAttribute("roadmap") RoadmapRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails != null && userDetails.getUser() != null) {
            requestDTO.setInstructorId(userDetails.getUser().getId());
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roadmap", bindingResult);
            redirectAttributes.addFlashAttribute("roadmap", requestDTO);
            return "redirect:/teacher/roadmap/create";
        }
        try {
            roadmapService.create(requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Roadmap created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("roadmap", requestDTO);
            return "redirect:/teacher/roadmap/create";
        }
        return "redirect:/teacher/roadmap";
    }

    @PostMapping("/roadmap/edit/{id}")
    public String updateRoadmap(@PathVariable Integer id,
                                 @Valid @ModelAttribute("roadmap") RoadmapRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails != null && userDetails.getUser() != null) {
            requestDTO.setInstructorId(userDetails.getUser().getId());
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roadmap", bindingResult);
            redirectAttributes.addFlashAttribute("roadmap", requestDTO);
            return "redirect:/teacher/roadmap/edit/" + id;
        }
        try {
            roadmapService.update(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Roadmap updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("roadmap", requestDTO);
            return "redirect:/teacher/roadmap/edit/" + id;
        }
        return "redirect:/teacher/roadmap";
    }

    @PostMapping("/roadmap/delete/{id}")
    public String deleteRoadmap(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            roadmapService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Roadmap deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/roadmap";
    }

    @PostMapping("/lessons")
    public ResponseEntity<?> createLesson() {
        return ResponseEntity.ok(Map.of("message", "Create lesson placeholder - not yet implemented"));
    }

    @PatchMapping("/lessons/{id}")
    public ResponseEntity<?> updateLesson(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update lesson " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete lesson " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/videos")
    public ResponseEntity<?> uploadVideo() {
        return ResponseEntity.ok(Map.of("message", "Upload video placeholder - not yet implemented"));
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete video " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/quizzes")
    public ResponseEntity<?> createQuiz() {
        return ResponseEntity.ok(Map.of("message", "Create quiz placeholder - not yet implemented"));
    }

    @PatchMapping("/quizzes/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update quiz " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete quiz " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/testcases")
    public ResponseEntity<?> createTestcase() {
        return ResponseEntity.ok(Map.of("message", "Create testcase placeholder - not yet implemented"));
    }

    @PatchMapping("/testcases/{id}")
    public ResponseEntity<?> updateTestcase(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update testcase " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/testcases/{id}")
    public ResponseEntity<?> deleteTestcase(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete testcase " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/assignments/{id}/grade")
    public ResponseEntity<?> gradeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Grade assignment " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/blogs")
    public String createBlog(@Valid @ModelAttribute("blogPost") BlogPostRequestDTO requestDTO,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.blogPost", bindingResult);
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/teacher/blogs/editor";
        }
        try {
            blogPostService.createDraft(requestDTO, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog draft saved successfully.");
            return "redirect:/teacher/blogs";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/teacher/blogs/editor";
        }
    }

    @PatchMapping("/blogs/{id}")
    public ResponseEntity<?> updateBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update blog " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/blogs/{id}/edit")
    public String updateRejectedBlog(@PathVariable Integer id,
                                     @Valid @ModelAttribute("blogPost") BlogPostRequestDTO requestDTO,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.blogPost", bindingResult);
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/teacher/blogs/editor/" + id;
        }
        try {
            blogPostService.updateRejected(id, requestDTO, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Rejected blog updated. Submit it again when ready.");
            return "redirect:/teacher/blogs";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/teacher/blogs/editor/" + id;
        }
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete blog " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/blogs/{id}/submit")
    public String submitBlog(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            blogPostService.submitForReview(id, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog submitted for review.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/blogs";
    }

    @PostMapping("/lessons/create")
    public String createLesson(@Valid @ModelAttribute("lesson") LessonRequestDTO requestDTO, 
                               BindingResult bindingResult, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.lesson", bindingResult);
            redirectAttributes.addFlashAttribute("lesson", requestDTO);
            return "redirect:/teacher/lessons/create?courseId=" + requestDTO.getCourseId();
        }
        try {
            LessonResponseDTO lesson = lessonService.create(requestDTO, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Lesson created successfully!");
            if (lesson.getType() == LessonType.QUIZ) {
                return "redirect:/teacher/quizzes?courseId=" + lesson.getCourseId() + "&lessonId=" + lesson.getId();
            }
            if (lesson.getType() == LessonType.CODING) {
                return "redirect:/teacher/assignments?courseId=" + lesson.getCourseId() + "&lessonId=" + lesson.getId();
            }
            if (lesson.getType() == LessonType.VIDEO) {
                return "redirect:/teacher/videos/create?courseId=" + lesson.getCourseId() + "&lessonId=" + lesson.getId();
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("lesson", requestDTO);
            return "redirect:/teacher/lessons/create?courseId=" + requestDTO.getCourseId();
        }
        return "redirect:/teacher/lessons?courseId=" + requestDTO.getCourseId();
    }

    @PostMapping("/lessons/edit/{id}")
    public String updateLesson(@PathVariable Integer id,
                               @Valid @ModelAttribute("lesson") LessonRequestDTO requestDTO, 
                               BindingResult bindingResult, 
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.lesson", bindingResult);
            redirectAttributes.addFlashAttribute("lesson", requestDTO);
            return "redirect:/teacher/lessons/edit/" + id + "?courseId=" + requestDTO.getCourseId();
        }
        try {
            lessonService.update(id, requestDTO, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Lesson updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("lesson", requestDTO);
            return "redirect:/teacher/lessons/edit/" + id + "?courseId=" + requestDTO.getCourseId();
        }
        return "redirect:/teacher/lessons?courseId=" + requestDTO.getCourseId();
    }

    @PostMapping("/lessons/delete/{id}")
    public String deleteLesson(@PathVariable Integer id, 
                               @RequestParam Integer courseId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        try {
            lessonService.delete(id, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Lesson deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/lessons?courseId=" + courseId;
    }

    @PostMapping("/lessons/reorder")
    @ResponseBody
    public ResponseEntity<?> reorderLessons(@RequestParam Integer courseId, 
                                            @RequestBody List<Integer> lessonIdsInOrder, 
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        try {
            lessonService.reorderLessons(courseId, lessonIdsInOrder, instructorId);
            return ResponseEntity.ok(Map.of("message", "Lessons reordered successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ----------------------------------------------------------------
    // Video / Media Management
    // ----------------------------------------------------------------

    @PostMapping("/videos/create")
    public String createVideo(@Valid @ModelAttribute("video") VideoRequestDTO requestDTO,
                              BindingResult bindingResult,
                              @RequestParam(required = false) Integer courseId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;
        Integer lessonId = requestDTO.getLessonId();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.video", bindingResult);
            redirectAttributes.addFlashAttribute("video", requestDTO);
            String redirect = "redirect:/teacher/videos/create?lessonId=" + lessonId;
            if (courseId != null) redirect += "&courseId=" + courseId;
            return redirect;
        }
        try {
            videoService.create(requestDTO, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Video added successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("video", requestDTO);
            String redirect = "redirect:/teacher/videos/create?lessonId=" + lessonId;
            if (courseId != null) redirect += "&courseId=" + courseId;
            return redirect;
        }
        String redirect = "redirect:/teacher/videos?lessonId=" + lessonId;
        if (courseId != null) redirect += "&courseId=" + courseId;
        return redirect;
    }

    @PostMapping("/videos/edit/{id}")
    public String updateVideo(@PathVariable Integer id,
                              @Valid @ModelAttribute("video") VideoRequestDTO requestDTO,
                              BindingResult bindingResult,
                              @RequestParam(required = false) Integer courseId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;
        Integer lessonId = requestDTO.getLessonId();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.video", bindingResult);
            redirectAttributes.addFlashAttribute("video", requestDTO);
            String redirect = "redirect:/teacher/videos/edit/" + id + "?lessonId=" + lessonId;
            if (courseId != null) redirect += "&courseId=" + courseId;
            return redirect;
        }
        try {
            videoService.update(id, requestDTO, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Video updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("video", requestDTO);
            String redirect = "redirect:/teacher/videos/edit/" + id + "?lessonId=" + lessonId;
            if (courseId != null) redirect += "&courseId=" + courseId;
            return redirect;
        }
        String redirect = "redirect:/teacher/videos?lessonId=" + lessonId;
        if (courseId != null) redirect += "&courseId=" + courseId;
        return redirect;
    }

    @PostMapping("/videos/delete/{id}")
    public String deleteVideo(@PathVariable Integer id,
                              @RequestParam Integer lessonId,
                              @RequestParam(required = false) Integer courseId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;
        try {
            videoService.delete(id, instructorId);
            redirectAttributes.addFlashAttribute("successMessage", "Video deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String redirect = "redirect:/teacher/videos?lessonId=" + lessonId;
        if (courseId != null) redirect += "&courseId=" + courseId;
        return redirect;
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUser();
    }
}
