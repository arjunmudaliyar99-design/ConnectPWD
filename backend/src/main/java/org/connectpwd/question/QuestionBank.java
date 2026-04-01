package org.connectpwd.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.question.dto.QuestionDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
@Getter
public class QuestionBank {

    private final Map<Integer, List<QuestionItem>> levelQuestions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadQuestions() {
        loadLevel(1, "questions/level1_intake.json");
        loadLevel(2, "questions/level2_isaa.json");
        loadLevel(3, "questions/level3_therapy.json");
        loadLevel(4, "questions/level4_vocational.json");
        log.info("Question bank loaded: L1={}, L2={}, L3={}, L4={}",
                getLevelSize(1), getLevelSize(2), getLevelSize(3), getLevelSize(4));
    }

    private void loadLevel(int level, String path) {
        try {
            InputStream is = new ClassPathResource(path).getInputStream();
            List<QuestionItem> items = objectMapper.readValue(is, new TypeReference<>() {});
            levelQuestions.put(level, items);
        } catch (IOException e) {
            log.error("Failed to load question file: {}", path, e);
            throw new RuntimeException("Failed to load question bank: " + path, e);
        }
    }

    public int getLevelSize(int level) {
        return levelQuestions.getOrDefault(level, Collections.emptyList()).size();
    }

    public QuestionItem getQuestion(int level, int questionIndex) {
        List<QuestionItem> questions = levelQuestions.get(level);
        if (questions == null || questionIndex < 0 || questionIndex >= questions.size()) {
            return null;
        }
        return questions.get(questionIndex);
    }

    public QuestionDTO toDTO(int level, int questionIndex, String language) {
        QuestionItem item = getQuestion(level, questionIndex);
        if (item == null) {
            return null;
        }

        List<QuestionItem> questions = levelQuestions.get(level);
        boolean isHindi = "hi".equals(language);

        String domainName = isHindi ? item.getDomainNameHi() : item.getDomainNameEn();
        boolean isFirstInDomain = questionIndex == 0 ||
                questions.get(questionIndex - 1).getDomainIndex() != item.getDomainIndex();
        boolean isLastInLevel = questionIndex == questions.size() - 1;

        return QuestionDTO.builder()
                .index(item.getIndex())
                .code(item.getCode())
                .level(level)
                .domain(domainName)
                .type(item.getType())
                .text(isHindi ? item.getTextHi() : item.getTextEn())
                .description(isHindi ? item.getDescriptionHi() : item.getDescriptionEn())
                .options(isHindi ? item.getOptionsHi() : item.getOptionsEn())
                .isFirstInDomain(isFirstInDomain)
                .isLastInLevel(isLastInLevel)
                .totalInLevel(questions.size())
                .currentPositionInLevel(questionIndex + 1)
                .build();
    }

    public QuestionItem findByCode(int level, String code) {
        List<QuestionItem> questions = levelQuestions.get(level);
        if (questions == null) return null;
        return questions.stream()
                .filter(q -> q.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public int findIndexByCode(int level, String code) {
        List<QuestionItem> questions = levelQuestions.get(level);
        if (questions == null) return -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getCode().equals(code)) {
                return i;
            }
        }
        return -1;
    }
}
