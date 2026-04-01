package org.connectpwd.scoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsaaScoreDTO {
    private UUID id;
    private UUID sessionId;
    private int totalScore;
    private String severity;
    private int disabilityPct;
    private int domain1Social;
    private int domain2Emotional;
    private int domain3Speech;
    private int domain4Behaviour;
    private int domain5Sensory;
    private int domain6Cognitive;
    private Instant scoredAt;
}
