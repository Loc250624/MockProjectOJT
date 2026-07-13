package com.ojtsu26.elearning.dto.request;

import lombok.Data;

@Data
public class VideoProgressRequestDTO {
    private Integer watchedSeconds;
    private Integer currentTimeSeconds;
    private Integer maxReachedSeconds;
    private Integer durationSeconds;
    private String eventType;
}
