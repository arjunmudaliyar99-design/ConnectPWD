package org.connectpwd.answer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.connectpwd.answer.dto.AnswerResponse;
import org.connectpwd.answer.dto.TextAnswerRequest;
import org.connectpwd.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/text")
    public ResponseEntity<ApiResponse<AnswerResponse>> submitTextAnswer(
            @Valid @RequestBody TextAnswerRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        AnswerResponse response = answerService.submitTextAnswer(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<AnswerResponse>> submitVoiceAnswer(
            @RequestParam String sessionId,
            @RequestParam String questionCode,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(required = false) String transcript,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        AnswerResponse response = answerService.submitVoiceAnswer(userId, sessionId, questionCode, audio, transcript);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
