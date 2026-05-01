package org.connectpwd.session;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "assessment_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSession {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    @Field("module_type")
    private String moduleType;

    @Field("triage_seeking_for")
    private String triageSeekingFor;

    @Field("triage_age")
    private Integer triageAge;

    @Field("triage_challenge_type")
    private String triageChallengeType;

    @Field("current_level")
    @Builder.Default
    private int currentLevel = 1;

    @Field("current_question_index")
    @Builder.Default
    private int currentQuestionIndex = 0;

    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Builder.Default
    private String language = "en";

    @Field("started_at")
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Field("completed_at")
    private Instant completedAt;
}
