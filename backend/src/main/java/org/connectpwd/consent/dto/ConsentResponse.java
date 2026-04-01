package org.connectpwd.consent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {
    private UUID id;
    private String clientName;
    private LocalDate clientDob;
    private String legalName;
    private String relationship;
    private boolean agreed;
    private Instant signedAt;
}
