package org.connectpwd.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleQuestion {

    private String id;
    private String text;
    private QuestionResponseType responseType;
    private List<String> options;
    private String sectionId;
    private String sectionTitle;
    private String sectionSubtitle;
    private int flatIndex;
}
