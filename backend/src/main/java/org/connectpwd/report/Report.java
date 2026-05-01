package org.connectpwd.report;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("session_id")
    private String sessionId;

    @Field("isaa_score_id")
    private String isaaScoreId;

    @Field("pdf_url")
    private String pdfUrl;

    private String language;

    @Field("generated_at")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
