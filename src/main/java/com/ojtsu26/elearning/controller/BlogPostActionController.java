package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.UserFacingErrorMessage;
import com.ojtsu26.elearning.dto.request.BlogPostRequestDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.BlogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BlogPostActionController {

    private final BlogPostService blogPostService;

    @PostMapping("/student/blogs")
    public String createStudentBlog(@Valid @ModelAttribute("blogPost") BlogPostRequestDTO requestDTO,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.blogPost", bindingResult);
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/student/blogs/editor";
        }
        try {
            blogPostService.createDraft(requestDTO, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog draft saved successfully.");
            return "redirect:/student/blogs";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", UserFacingErrorMessage.from(e));
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/student/blogs/editor";
        }
    }

    @PostMapping("/student/blogs/{id}/submit")
    public String submitStudentBlog(@PathVariable Integer id,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            blogPostService.submitForReview(id, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog submitted for review.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", UserFacingErrorMessage.from(e));
        }
        return "redirect:/student/blogs";
    }

    @PostMapping("/student/blogs/{id}/edit")
    public String updateRejectedStudentBlog(@PathVariable Integer id,
                                            @Valid @ModelAttribute("blogPost") BlogPostRequestDTO requestDTO,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal CustomUserDetails userDetails,
                                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.blogPost", bindingResult);
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/student/blogs/editor/" + id;
        }
        try {
            blogPostService.updateRejected(id, requestDTO, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Rejected blog updated. Submit it again when ready.");
            return "redirect:/student/blogs";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", UserFacingErrorMessage.from(e));
            redirectAttributes.addFlashAttribute("blogPost", requestDTO);
            return "redirect:/student/blogs/editor/" + id;
        }
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUser();
    }
}
