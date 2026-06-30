package com.ojtsu26.elearning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminActionController {

    private final CategoryService categoryService;
    private final CourseService courseService;

    @PostMapping("/users")
    public ResponseEntity<?> createUser() {
        return ResponseEntity.ok(Map.of("message", "Create user placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update user " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete user " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Block user " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Unblock user " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/categories/create")
    public String createCategory(@Valid @ModelAttribute("category") CategoryRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/create";
        }
        try {
            categoryService.create(requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/create";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/edit/{id}")
    public String updateCategory(@PathVariable Integer id,
                                 @Valid @ModelAttribute("category") CategoryRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/edit/" + id;
        }
        try {
            categoryService.update(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/edit/" + id;
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ----------------------------------------------------------------
    // CRS-09: Admin Course Approval / Governance
    // ----------------------------------------------------------------

    @PostMapping("/courses/approve/{id}")
    public String approveCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.approveCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course approved and published successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=PENDING_APPROVAL";
    }

    @PostMapping("/courses/reject/{id}")
    public String rejectCourse(@PathVariable Integer id,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) String rejectReason,
                               RedirectAttributes redirectAttributes) {
        try {
            courseService.rejectCourse(id, rejectReason);
            redirectAttributes.addFlashAttribute("successMessage", "Course rejected. The teacher will be notified.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=PENDING_APPROVAL";
    }

    @PostMapping("/courses/hide/{id}")
    public String hideCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.hideCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course is now hidden from public catalog.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=APPROVED";
    }

    @PostMapping("/courses/unhide/{id}")
    public String unhideCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.unhideCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course is now visible in the public catalog.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=HIDDEN";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted permanently.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval";
    }

    @PatchMapping("/blogs/{id}/approve")
    public ResponseEntity<?> approveBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Approve blog " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/blogs/{id}/reject")
    public ResponseEntity<?> rejectBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Reject blog " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete blog " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete comment " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/refunds")
    public ResponseEntity<?> createRefund() {
        return ResponseEntity.ok(Map.of("message", "Create refund placeholder - not yet implemented"));
    }

    @PatchMapping("/system-settings")
    public ResponseEntity<?> updateSystemSettings() {
        return ResponseEntity.ok(Map.of("message", "Update system settings placeholder - not yet implemented"));
    }
}
