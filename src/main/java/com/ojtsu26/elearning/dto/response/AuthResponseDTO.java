package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    @ToString.Exclude
    private String token;
    @Builder.Default
    private String type = "Bearer";
    @ToString.Exclude
    private Integer id;
    @ToString.Exclude
    private String email;
    @ToString.Exclude
    private String fullName;
    private String role;
    private String redirectUrl;
}
