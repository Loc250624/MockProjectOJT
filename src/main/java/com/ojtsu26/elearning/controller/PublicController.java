package com.ojtsu26.elearning.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @GetMapping("/")
    public String homePage() {
        return "public/home";
    }

    @GetMapping("/courses")
    public String coursesPage() {
        return "public/courses";
    }

    @GetMapping("/courses/detail")
    public String courseDetailPage() {
        return "public/course-detail";
    }

    @GetMapping("/blogs")
    public String blogsPage() {
        return "public/blogs";
    }

    @GetMapping("/blogs/detail")
    public String blogDetailPage() {
        return "public/blog-detail";
    }
}
