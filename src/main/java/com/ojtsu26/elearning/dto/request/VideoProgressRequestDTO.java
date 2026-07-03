package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoProgressRequestDTO {
    @NotNull(message = "Watched seconds is required")
    private Integer watchedSeconds;
}
