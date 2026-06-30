package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;

@Data
public class VideoRequestDTO {

    @NotBlank(message = "Video URL is required")
    @Pattern(
        regexp = "^https?://.*",
        message = "Video URL must be a valid HTTP or HTTPS URL"
    )
    private String videoUrl;

    @NotNull(message = "Duration in seconds is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    @NotNull(message = "Lesson ID is required")
    private Integer lessonId;
}
