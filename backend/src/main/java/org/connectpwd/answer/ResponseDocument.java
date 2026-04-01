package org.connectpwd.answer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "responses")
@CompoundIndex(name = "session_question_idx", def = "{'sessionId': 1, 'questionIndex': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDocument {

    @Id
    private String id;

    private String sessionId;
    private int level;
    private int questionIndex;
    private String questionCode;
    private String domain;
    private String questionText;
    private String answerType;
    private String answerText;
    private Integer scaleValue;
    private String audioKey;
    private String transcript;

    @Builder.Default
    private Instant answeredAt = Instant.now();
}
