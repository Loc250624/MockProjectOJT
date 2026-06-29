package com.ojtsu26.elearning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentActionController {

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile() {
        return ResponseEntity.ok(Map.of("message", "Update profile placeholder - not yet implemented"));
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<?> enrollCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Enroll in course " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/quiz/{id}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Submit quiz " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/code-assignment/{id}/submit")
    public ResponseEntity<?> submitCodeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Submit code assignment " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout() {
        return ResponseEntity.ok(Map.of("message", "Checkout placeholder - not yet implemented"));
    }

    @PostMapping("/blogs/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Add comment to blog " + id + " placeholder - not yet implemented"));
    }
}
