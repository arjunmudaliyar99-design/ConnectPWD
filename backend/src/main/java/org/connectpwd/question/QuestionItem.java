package org.connectpwd.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionItem {

    private int index;
    private String code;

    @JsonProperty("text_en")
    private String textEn;

    @JsonProperty("text_hi")
    private String textHi;

    @JsonProperty("description_en")
    private String descriptionEn;

    @JsonProperty("description_hi")
    private String descriptionHi;

    private String type;
    private int domainIndex;

    @JsonProperty("domainName_en")
    private String domainNameEn;

    @JsonProperty("domainName_hi")
    private String domainNameHi;

    @JsonProperty("options_en")
    private List<String> optionsEn;

    @JsonProperty("options_hi")
    private List<String> optionsHi;
}
