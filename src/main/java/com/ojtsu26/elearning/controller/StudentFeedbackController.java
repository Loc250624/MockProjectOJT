package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.dto.request.StudentFeedbackRequestDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.service.StudentFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class StudentFeedbackController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final List<Integer> RATING_OPTIONS_DESCENDING = List.of(5, 4, 3, 2, 1);

    private final StudentFeedbackService feedbackService;

    @InitBinder("feedback")
    void initFeedbackBinder(WebDataBinder binder) {
        binder.setAllowedFields(
                "subject",
                "content",
                "courseContentRating",
                "instructorSupportRating",
                "learningExperienceRating",
                "platformUsabilityRating",
                "assessmentExperienceRating",
                "overallSatisfactionRating"
        );
    }

    @GetMapping("/student/feedback")
    public String studentFeedbackForm(Model model) {
        if (!model.containsAttribute("feedback")) {
            model.addAttribute("feedback", new StudentFeedbackRequestDTO());
        }
        addStudentFormOptions(model);
        return "student/feedback";
    }

    @PostMapping("/student/feedback")
    public String createStudentFeedback(@Valid @ModelAttribute("feedback") StudentFeedbackRequestDTO request,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.feedback", bindingResult);
            redirectAttributes.addFlashAttribute("feedback", request);
            return "redirect:/student/feedback";
        }

        try {
            feedbackService.createForCurrentStudent(request);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("feedback", request);
            return "redirect:/student/feedback";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Feedback submitted successfully.");
        return "redirect:/student/feedback";
    }

    @GetMapping("/admin/feedback")
    public String adminFeedbackList(@RequestParam(value = "page", defaultValue = "0") int page,
                                    Model model) {
        model.addAttribute("feedbackPage", feedbackService.getAllFeedbacksForAdmin(page, DEFAULT_PAGE_SIZE));
        return "admin/feedback";
    }

    @GetMapping("/admin/feedback/{id}")
    public String adminFeedbackDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("feedback", feedbackService.getFeedbackDetailForAdmin(id));
        return "admin/feedback-detail";
    }

    private void addStudentFormOptions(Model model) {
        model.addAttribute("ratingOptionsDescending", RATING_OPTIONS_DESCENDING);
    }
}
