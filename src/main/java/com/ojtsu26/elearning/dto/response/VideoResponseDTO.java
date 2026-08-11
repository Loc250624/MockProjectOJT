package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class VideoResponseDTO {
    private Integer id;

    private VideoSourceType sourceType;

    private String videoUrl;

    private Integer durationSeconds;

    private String originalFilename;

    private String contentType;

    private Long fileSizeBytes;

    private Integer lessonId;
}
