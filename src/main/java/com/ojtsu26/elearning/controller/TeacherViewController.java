package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherViewController {

    private final ProfileService profileService;

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
    public String courses() { return "teacher/courses"; }

    @GetMapping("/courses/create")
    public String courseCreate() { return "teacher/course-form"; }

    @GetMapping("/courses/edit")
    public String courseEdit() { return "teacher/course-form"; }

    @GetMapping("/roadmap")
    public String roadmap() { return "teacher/roadmap"; }

    @GetMapping("/lessons")
    public String lessons() { return "teacher/lessons"; }

    @GetMapping("/lessons/editor")
    public String lessonEditor() { return "teacher/lesson-editor"; }

    @GetMapping("/videos")
    public String videos() { return "teacher/videos"; }

    @GetMapping("/students")
    public String students() { return "teacher/students"; }

    @GetMapping("/quizzes")
    public String quizzes() { return "teacher/quizzes"; }

    @GetMapping("/quizzes/create")
    public String quizCreate() { return "teacher/quiz-form"; }

    @GetMapping("/quizzes/edit")
    public String quizEdit() { return "teacher/quiz-form"; }

    @GetMapping("/testcases")
    public String testcases() { return "teacher/testcases"; }

    @GetMapping("/assignments")
    public String assignments() { return "teacher/assignments"; }

    @GetMapping("/grading")
    public String grading() { return "teacher/grading"; }

    @GetMapping("/blogs")
    public String blogs() { return "teacher/blogs"; }

    @GetMapping("/blogs/editor")
    public String blogEditor() { return "teacher/blog-editor"; }

    @GetMapping("/blogs/submissions")
    public String blogSubmissions() { return "teacher/blog-submissions"; }

    @GetMapping("/analytics")
    public String analytics() { return "teacher/analytics"; }

    @GetMapping("/reports/progress")
    public String progressReport() { return "teacher/progress-report"; }
}
