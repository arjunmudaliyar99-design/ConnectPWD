package org.connectpwd.consent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    @NotBlank(message = "Client name is required")
    @Size(max = 200, message = "Client name must not exceed 200 characters")
    private String clientName;

    @NotNull(message = "Client date of birth is required")
    private LocalDate clientDob;

    @NotBlank(message = "Legal name (e-signature) is required")
    @Size(max = 200, message = "Legal name must not exceed 200 characters")
    private String legalName;

    @NotBlank(message = "Relationship is required")
    @Size(max = 100, message = "Relationship must not exceed 100 characters")
    private String relationship;

    @NotNull(message = "Agreement is required")
    private Boolean agreed;
}
