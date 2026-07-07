package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.ojtsu26.elearning.service.RoadmapService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.LessonService;
import com.ojtsu26.elearning.service.TeacherCourseStudentService;
import com.ojtsu26.elearning.service.VideoService;
import com.ojtsu26.elearning.service.AssessmentService;
import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping({"/teacher", "/instructor"})
@RequiredArgsConstructor
public class TeacherViewController {

    private final RoadmapService roadmapService;
    private final CourseService courseService;
    private final CategoryService categoryService;
    private final LessonService lessonService;
    private final VideoService videoService;
    private final ProfileService profileService;
    private final TeacherCourseStudentService teacherCourseStudentService;
    private final AssessmentService assessmentService;

    @GetMapping("/dashboard")
    public String dashboard() { return "teacher/dashboard"; }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("profile", profileService.getCurrentProfile());
        model.addAttribute("overview", profileService.getCurrentProfileOverview());
        model.addAttribute("profileRole", "teacher");
        model.addAttribute("profilePortalName", "Teacher Portal");
        return "student/profile";
    }

    @GetMapping("/courses")
    public String courses(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("courses", courseService.findByInstructorId(userDetails.getUser().getId()));
        }
        return "teacher/courses";
    }

    @GetMapping("/courses/create")
    public String courseCreate(Model model) {
        if (!model.containsAttribute("course")) {
            model.addAttribute("course", new CourseRequestDTO());
        }
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("roadmaps", roadmapService.findAll());
        return "teacher/course-form";
    }

    @GetMapping("/courses/edit/{id}")
    public String courseEdit(@PathVariable Integer id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            CourseResponseDTO responseDTO = courseService.findById(id);
            
            // Ownership check
            if (userDetails == null || userDetails.getUser() == null || !responseDTO.getInstructorId().equals(userDetails.getUser().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: You do not own this course.");
                return "redirect:/teacher/courses";
            }

            if (!model.containsAttribute("course")) {
                CourseRequestDTO requestDTO = new CourseRequestDTO();
                requestDTO.setTitle(responseDTO.getTitle());
                requestDTO.setDescription(responseDTO.getDescription());
                requestDTO.setThumbnailUrl(responseDTO.getThumbnailUrl());
                requestDTO.setPrice(responseDTO.getPrice());
                requestDTO.setStatus(responseDTO.getStatus());
                requestDTO.setInstructorId(responseDTO.getInstructorId());
                requestDTO.setCategoryId(responseDTO.getCategoryId());
                requestDTO.setRoadmapId(responseDTO.getRoadmapId());
                model.addAttribute("course", requestDTO);
            }
            model.addAttribute("editMode", true);
            model.addAttribute("courseId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("roadmaps", roadmapService.findAll());
            return "teacher/course-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found: " + e.getMessage());
            return "redirect:/teacher/courses";
        }
    }

    @GetMapping("/roadmap")
    public String roadmap(Model model) {
        model.addAttribute("roadmaps", roadmapService.findAll());
        return "teacher/roadmap";
    }

    @GetMapping("/roadmap/create")
    public String roadmapCreate(Model model) {
        if (!model.containsAttribute("roadmap")) {
            model.addAttribute("roadmap", new RoadmapRequestDTO());
        }
        return "teacher/roadmap-form";
    }

    @GetMapping("/roadmap/edit/{id}")
    public String roadmapEdit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("roadmap")) {
                RoadmapResponseDTO responseDTO = roadmapService.findById(id);
                RoadmapRequestDTO requestDTO = new RoadmapRequestDTO();
                requestDTO.setTitle(responseDTO.getTitle());
                requestDTO.setDescription(responseDTO.getDescription());
                if (responseDTO.getInstructorId() != null) {
                    requestDTO.setInstructorId(responseDTO.getInstructorId());
                }
                model.addAttribute("roadmap", requestDTO);
            }
            model.addAttribute("editMode", true);
            model.addAttribute("roadmapId", id);
            return "teacher/roadmap-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap not found: " + e.getMessage());
            return "redirect:/teacher/roadmap";
        }
    }

    @GetMapping("/lessons")
    public String lessons(@RequestParam(required = false) Integer courseId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            Integer instructorId = userDetails.getUser().getId();
            // List of courses for the dropdown
            model.addAttribute("courses", courseService.findByInstructorId(instructorId));
            
            if (courseId != null) {
                try {
                    model.addAttribute("lessons", lessonService.findByCourseId(courseId, instructorId));
                    model.addAttribute("selectedCourseId", courseId);
                    model.addAttribute("selectedCourse", courseService.findById(courseId));
                } catch (Exception e) {
                    model.addAttribute("errorMessage", e.getMessage());
                }
            }
        }
        return "teacher/lessons";
    }

    @GetMapping("/lessons/create")
    public String lessonCreate(@RequestParam Integer courseId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        try {
            if (!model.containsAttribute("lesson")) {
                LessonRequestDTO req = new LessonRequestDTO();
                req.setCourseId(courseId);
                model.addAttribute("lesson", req);
            }
            model.addAttribute("courseId", courseId);
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("lessons", lessonService.findByCourseId(courseId, instructorId));
            model.addAttribute("lessonTypes", LessonType.values());
            return "teacher/lesson-editor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found or access denied: " + e.getMessage());
            return "redirect:/teacher/lessons";
        }
    }

    @GetMapping("/lessons/edit/{id}")
    public String lessonEdit(@PathVariable Integer id, @RequestParam Integer courseId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null) ? userDetails.getUser().getId() : null;
        try {
            if (!model.containsAttribute("lesson")) {
                LessonResponseDTO res = lessonService.findById(id);
                LessonRequestDTO req = new LessonRequestDTO();
                req.setTitle(res.getTitle());
                req.setContent(res.getContent());
                req.setType(res.getType());
                req.setOrderIndex(res.getOrderIndex());
                req.setCourseId(courseId);
                model.addAttribute("lesson", req);
            }
            model.addAttribute("editMode", true);
            model.addAttribute("lessonId", id);
            model.addAttribute("courseId", courseId);
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("lessons", lessonService.findByCourseId(courseId, instructorId));
            model.addAttribute("lessonTypes", LessonType.values());
            return "teacher/lesson-editor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lesson not found or access denied: " + e.getMessage());
            return "redirect:/teacher/lessons?courseId=" + courseId;
        }
    }

    // ----------------------------------------------------------------
    // Video / Media Management
    // ----------------------------------------------------------------

    /**
     * GET /teacher/videos?lessonId=&courseId=
     * Shows the video attached to a lesson, or a prompt to add one.
     */
    @GetMapping("/videos")
    public String videos(@RequestParam(required = false) Integer lessonId,
                         @RequestParam(required = false) Integer courseId,
                         Model model,
                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;

        // Populate course list for lesson selector
        if (instructorId != null) {
            model.addAttribute("courses", courseService.findByInstructorId(instructorId));
        }

        if (courseId != null) {
            try {
                model.addAttribute("lessons", lessonService.findByCourseId(courseId, instructorId));
                model.addAttribute("selectedCourseId", courseId);
                model.addAttribute("selectedCourse", courseService.findById(courseId));
            } catch (Exception ignored) {}
        }

        if (lessonId != null) {
            model.addAttribute("selectedLessonId", lessonId);
            try {
                model.addAttribute("selectedLesson", lessonService.findById(lessonId));
                Optional<VideoResponseDTO> videoOpt = videoService.findByLessonId(lessonId, instructorId);
                videoOpt.ifPresent(v -> model.addAttribute("video", v));
                model.addAttribute("hasVideo", videoOpt.isPresent());
            } catch (Exception e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
        }

        return "teacher/videos";
    }

    /**
     * GET /teacher/videos/create?lessonId=&courseId=
     * Shows the form to add a video URL to a lesson.
     * Redirects to edit if the lesson already has a video.
     */
    @GetMapping("/videos/create")
    public String videoCreate(@RequestParam Integer lessonId,
                              @RequestParam Integer courseId,
                              Model model,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;

        // If lesson already has a video, redirect to edit
        try {
            Optional<VideoResponseDTO> existing = videoService.findByLessonId(lessonId, instructorId);
            if (existing.isPresent()) {
                redirectAttributes.addFlashAttribute("infoMessage",
                    "This lesson already has a video. You can edit it below.");
                return "redirect:/teacher/videos/edit/" + existing.get().getId()
                        + "?lessonId=" + lessonId + "&courseId=" + courseId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/videos?lessonId=" + lessonId + "&courseId=" + courseId;
        }

        if (!model.containsAttribute("video")) {
            VideoRequestDTO req = new VideoRequestDTO();
            req.setLessonId(lessonId);
            model.addAttribute("video", req);
        }
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("courseId", courseId);
        try {
            model.addAttribute("selectedLesson", lessonService.findById(lessonId));
            model.addAttribute("selectedCourse", courseService.findById(courseId));
        } catch (Exception ignored) {}

        return "teacher/video-form";
    }

    /**
     * GET /teacher/videos/edit/{id}?lessonId=&courseId=
     * Shows the form pre-populated with the existing video data.
     */
    @GetMapping("/videos/edit/{id}")
    public String videoEdit(@PathVariable Integer id,
                            @RequestParam Integer lessonId,
                            @RequestParam Integer courseId,
                            Model model,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer instructorId = (userDetails != null && userDetails.getUser() != null)
                ? userDetails.getUser().getId() : null;

        if (!model.containsAttribute("video")) {
            try {
                VideoResponseDTO res = videoService.findById(id);
                VideoRequestDTO req = new VideoRequestDTO();
                req.setVideoUrl(res.getVideoUrl());
                req.setDurationSeconds(res.getDurationSeconds());
                req.setLessonId(lessonId);
                model.addAttribute("video", req);
            } catch (Exception e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
        }
        model.addAttribute("editMode", true);
        model.addAttribute("videoId", id);
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("courseId", courseId);
        try {
            model.addAttribute("selectedLesson", lessonService.findById(lessonId));
            model.addAttribute("selectedCourse", courseService.findById(courseId));
        } catch (Exception ignored) {}

        return "teacher/video-form";
    }

    @GetMapping("/students")
    public String students(@RequestParam(required = false) Integer courseId,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String enrollmentStatus,
                           @RequestParam(required = false) String progressState,
                           @RequestParam(required = false) String sort,
                           @RequestParam(required = false) String direction,
                           @RequestParam(required = false) Integer page,
                           @RequestParam(required = false) Integer size,
                           Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            Integer instructorId = userDetails.getUser().getId();
            model.addAttribute("courses", courseService.findByInstructorId(instructorId));
            model.addAttribute("selectedCourseId", courseId);
            if (courseId != null) {
                try {
                    model.addAttribute("studentPage", teacherCourseStudentService.findStudentsForCurrentTeacherCourse(
                            courseId,
                            search,
                            enrollmentStatus,
                            progressState,
                            null,
                            null,
                            sort,
                            direction,
                            page,
                            size
                    ));
                } catch (RuntimeException e) {
                    model.addAttribute("errorMessage", e.getMessage());
                }
            }
        }
        return "teacher/students";
    }

    @GetMapping("/quizzes")
    public String quizzes() { return "teacher/quizzes"; }

    @GetMapping("/courses/{courseId}/assessments/quizzes")
    public String courseQuizzes(@PathVariable Integer courseId, Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("quizzes", assessmentService.getTeacherCourseQuizzes(courseId));
        return "teacher/quizzes";
    }

    @GetMapping("/quizzes/create")
    public String quizCreate() { return "teacher/quiz-form"; }

    @GetMapping("/quizzes/edit")
    public String quizEdit() { return "teacher/quiz-form"; }

    @GetMapping("/testcases")
    public String testcases() { return "teacher/testcases"; }

    @GetMapping("/assignments/{assignmentId}/testcases")
    public String assignmentTestcases(@PathVariable Integer assignmentId, Model model) {
        model.addAttribute("assignmentId", assignmentId);
        model.addAttribute("testcases", assessmentService.getTeacherTestcases(assignmentId));
        return "teacher/testcases";
    }

    @GetMapping("/assignments")
    public String assignments() { return "teacher/assignments"; }

    @GetMapping("/grading")
    public String grading() { return "teacher/grading"; }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public String assignmentSubmissions(@PathVariable Integer assignmentId, Model model) {
        model.addAttribute("assignmentId", assignmentId);
        model.addAttribute("submissions", assessmentService.getTeacherAssignmentSubmissions(assignmentId));
        return "teacher/grading";
    }

    @GetMapping("/blogs")
    public String blogs() { return "teacher/blogs"; }

    @GetMapping("/blogs/editor")
    public String blogEditor() { return "teacher/blog-editor"; }

    @GetMapping("/blogs/submissions")
    public String blogSubmissions() { return "teacher/blog-submissions"; }

    @GetMapping("/analytics")
    public String analytics() { return "teacher/analytics"; }

    @GetMapping("/reports/progress")
    public String progressReport(@RequestParam(required = false) Integer courseId,
                                 @RequestParam(required = false) Integer studentId,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String enrollmentStatus,
                                 @RequestParam(required = false) String progressState,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastActivityFrom,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastActivityTo,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(required = false) String direction,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size,
                                 Model model,
                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            Integer instructorId = userDetails.getUser().getId();
            model.addAttribute("courses", courseService.findByInstructorId(instructorId));
            model.addAttribute("selectedCourseId", courseId);
            model.addAttribute("selectedStudentId", studentId);
            if (courseId != null) {
                try {
                    model.addAttribute("overview", teacherCourseStudentService.getProgressOverviewForCurrentTeacherCourse(courseId));
                    model.addAttribute("studentPage", teacherCourseStudentService.findStudentsForCurrentTeacherCourse(
                            courseId,
                            search,
                            enrollmentStatus,
                            progressState,
                            lastActivityFrom,
                            lastActivityTo,
                            sort,
                            direction,
                            page,
                            size
                    ));
                    if (studentId != null) {
                        model.addAttribute("studentDetail", teacherCourseStudentService.getStudentProgressDetailForCurrentTeacherCourse(courseId, studentId));
                    }
                } catch (RuntimeException e) {
                    model.addAttribute("errorMessage", e.getMessage());
                }
            }
        }
        return "teacher/progress-report";
    }
}
