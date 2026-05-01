package org.connectpwd.question;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleSection {

    private String sectionId;
    private String title;
    private String subtitle;
    private List<ModuleSectionQuestion> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModuleSectionQuestion {
        private String id;
        private String text;
        private String responseType;
        private List<String> options;
    }
}
