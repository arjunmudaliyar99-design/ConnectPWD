package org.connectpwd.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {

    @NotNull(message = "Consent ID is required")
    private UUID consentId;

    @Pattern(regexp = "^(en|hi)$", message = "Language must be 'en' or 'hi'")
    private String language;
}
