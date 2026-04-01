package org.connectpwd.session;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_id", nullable = false, unique = true)
    private UUID consentId;

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private int currentLevel = 1;

    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private int currentQuestionIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
