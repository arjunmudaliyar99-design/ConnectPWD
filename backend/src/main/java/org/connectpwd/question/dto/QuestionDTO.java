package org.connectpwd.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private int index;
    private String code;
    private int level;
    private String domain;
    private String type;
    private String text;
    private String description;
    private List<String> options;
    private boolean isFirstInDomain;
    private boolean isLastInLevel;
    private int totalInLevel;
    private int currentPositionInLevel;
}
