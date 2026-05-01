package org.connectpwd.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {

    @NotBlank(message = "moduleType is required")
    @Pattern(regexp = "^(PARENT|ADULT_SELF)$", message = "moduleType must be PARENT or ADULT_SELF")
    private String moduleType;

    @NotNull(message = "triageData is required")
    @Valid
    private TriageRequestDTO triageData;

    @Pattern(regexp = "^(en|hi)$", message = "Language must be 'en' or 'hi'")
    private String language;
}
