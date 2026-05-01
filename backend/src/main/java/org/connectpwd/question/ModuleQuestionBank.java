package org.connectpwd.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.question.dto.QuestionDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
@SuppressWarnings("null")
public class ModuleQuestionBank {

    private final Map<String, List<ModuleQuestion>> moduleQuestions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadModules() {
        loadModule("PARENT", "questions/parent_questionnaire.json");
        loadModule("ADULT_SELF", "questions/adult_questionnaire.json");
        log.info("Module question bank loaded: PARENT={}, ADULT_SELF={}",
                getTotalQuestions("PARENT"), getTotalQuestions("ADULT_SELF"));
    }

    private void loadModule(String moduleType, String path) {
        try {
            InputStream is = new ClassPathResource(path).getInputStream();
            ModuleQuestionnaireData data = objectMapper.readValue(is, ModuleQuestionnaireData.class);
            List<ModuleQuestion> flat = new ArrayList<>();
            int index = 0;
            for (ModuleSection section : data.getSections()) {
                for (ModuleSection.ModuleSectionQuestion q : section.getQuestions()) {
                    QuestionResponseType responseType = QuestionResponseType.valueOf(q.getResponseType());
                    ModuleQuestion mq = ModuleQuestion.builder()
                            .id(q.getId())
                            .text(q.getText())
                            .responseType(responseType)
                            .options(q.getOptions())
                            .sectionId(section.getSectionId())
                            .sectionTitle(section.getTitle())
                            .sectionSubtitle(section.getSubtitle())
                            .flatIndex(index)
                            .build();
                    flat.add(mq);
                    index++;
                }
            }
            moduleQuestions.put(moduleType, flat);
        } catch (IOException e) {
            log.error("Failed to load module question file: {}", path, e);
            throw new RuntimeException("Failed to load module question bank: " + path, e);
        }
    }

    public int getTotalQuestions(String moduleType) {
        return moduleQuestions.getOrDefault(moduleType, Collections.emptyList()).size();
    }

    public ModuleQuestion getQuestion(String moduleType, int index) {
        List<ModuleQuestion> questions = moduleQuestions.get(moduleType);
        if (questions == null || index < 0 || index >= questions.size()) {
            return null;
        }
        return questions.get(index);
    }

    public ModuleQuestion findById(String moduleType, String id) {
        List<ModuleQuestion> questions = moduleQuestions.get(moduleType);
        if (questions == null) return null;
        return questions.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public int findIndexById(String moduleType, String id) {
        List<ModuleQuestion> questions = moduleQuestions.get(moduleType);
        if (questions == null) return -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isFirstInSection(String moduleType, int index) {
        if (index == 0) return true;
        List<ModuleQuestion> questions = moduleQuestions.get(moduleType);
        if (questions == null || index >= questions.size()) return false;
        return !questions.get(index).getSectionId().equals(questions.get(index - 1).getSectionId());
    }

    public QuestionDTO toDTO(String moduleType, int index) {
        ModuleQuestion mq = getQuestion(moduleType, index);
        if (mq == null) return null;
        int total = getTotalQuestions(moduleType);
        return QuestionDTO.builder()
                .index(index)
                .code(mq.getId())
                .level(1)
                .domain(mq.getSectionTitle())
                .type(mq.getResponseType().name())
                .responseType(mq.getResponseType().name())
                .text(mq.getText())
                .options(mq.getOptions())
                .sectionId(mq.getSectionId())
                .sectionTitle(mq.getSectionTitle())
                .sectionSubtitle(mq.getSectionSubtitle())
                .isFirstInDomain(isFirstInSection(moduleType, index))
                .isLastInLevel(index == total - 1)
                .totalInLevel(total)
                .currentPositionInLevel(index + 1)
                .build();
    }
}
