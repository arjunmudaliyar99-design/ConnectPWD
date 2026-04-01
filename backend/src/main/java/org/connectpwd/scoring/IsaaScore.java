package org.connectpwd.scoring;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "isaa_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsaaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SeverityLevel severity;

    @Column(name = "disability_pct", nullable = false)
    private int disabilityPct;

    @Column(name = "domain1_social", nullable = false)
    private int domain1Social;

    @Column(name = "domain2_emotional", nullable = false)
    private int domain2Emotional;

    @Column(name = "domain3_speech", nullable = false)
    private int domain3Speech;

    @Column(name = "domain4_behaviour", nullable = false)
    private int domain4Behaviour;

    @Column(name = "domain5_sensory", nullable = false)
    private int domain5Sensory;

    @Column(name = "domain6_cognitive", nullable = false)
    private int domain6Cognitive;

    @Column(name = "scored_at", nullable = false)
    @Builder.Default
    private Instant scoredAt = Instant.now();
}
