package org.connectpwd.scoring;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "isaa_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsaaScore {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("session_id")
    private String sessionId;

    @Field("total_score")
    private int totalScore;

    private SeverityLevel severity;

    @Field("disability_pct")
    private int disabilityPct;

    @Field("domain1_social")
    private int domain1Social;

    @Field("domain2_emotional")
    private int domain2Emotional;

    @Field("domain3_speech")
    private int domain3Speech;

    @Field("domain4_behaviour")
    private int domain4Behaviour;

    @Field("domain5_sensory")
    private int domain5Sensory;

    @Field("domain6_cognitive")
    private int domain6Cognitive;

    @Field("scored_at")
    @Builder.Default
    private Instant scoredAt = Instant.now();
}
