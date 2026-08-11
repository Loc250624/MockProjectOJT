package com.ojtsu26.elearning.dto.request;

import com.ojtsu26.elearning.model.enums.VideoSourceType;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoRequestDTO {

    private VideoSourceType sourceType = VideoSourceType.YOUTUBE;

    private String videoUrl;

    private Integer durationSeconds;

    private MultipartFile videoFile;

    @NotNull(message = "Lesson ID is required")
    private Integer lessonId;
}
