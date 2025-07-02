package com.example.llm.controller;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.service.ConversationService;
import com.example.llm.service.LargeModelService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/llm")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LargeModelController {

    private final LargeModelService largeModelService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper; // 注入 ObjectMapper

    @Data
    public static class ChatRequestVariables {
        private String sessionId;
        private String uid;
        private String name;
    }


    /**
     * 获取流式 Large Model 响应，并在完成后保存 AI 回答到数据库
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public ResponseEntity<Flux<String>> askLargeModelStream(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        String chatId = (String) request.get("chatId");

        List<ConversationHistory> history = conversationService.getConversationHistoryBySessionId(sessionId);

        List<Map<String, Object>> fullMessages = new ArrayList<>(history.size() + 1);
        for (ConversationHistory h : history) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", h.getRole());
            msg.put("content", h.getContent());
            fullMessages.add(msg);
        }

        // 提取 message
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        String message = "";
        if (messages != null && !messages.isEmpty()) {
            Map<String, Object> lastMessage = messages.get(messages.size() - 1);
            message = (String) lastMessage.get("content");
        }

        // 添加当前用户消息
        Map<String, Object> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", message);
        fullMessages.add(currentMessage);

        // 调用模型服务
        Flux<String> responseFlux = largeModelService.streamLargeModelResponse(message, fullMessages, chatId, "", "");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(responseFlux);
    }


    /**
     * 文件上传
     * @param file
     * @param user
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("user") String user) {
        Map<String, Object> response = largeModelService.uploadFile(file, user);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取历史对话
     */
    @GetMapping("/history")
    public ResponseEntity<List<ConversationHistory>> getHistoryBySessionId(@RequestParam String sessionId) {
        List<ConversationHistory> history = conversationService.getConversationHistoryBySessionId(sessionId);
        return ResponseEntity.ok(history);
    }

}
