package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatRequestDTO {

    @NotNull(message = "Lesson ID is required.")
    private Integer lessonId;

    private String message;
    private String action;
    private List<AiTutorChatMessageDTO> history;
}
