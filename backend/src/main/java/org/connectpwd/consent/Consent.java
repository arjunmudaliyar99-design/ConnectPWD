package org.connectpwd.consent;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "consent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "client_dob", nullable = false)
    private LocalDate clientDob;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(nullable = false, length = 100)
    private String relationship;

    @Column(nullable = false)
    @Builder.Default
    private boolean agreed = false;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "signed_at", nullable = false)
    @Builder.Default
    private Instant signedAt = Instant.now();
}
