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
        String chatId = (String) request.get("chatId");
        boolean stream = Boolean.TRUE.equals(request.get("stream"));
        String responseChatItemId = (String) request.get("responseChatItemId");

        // 提取 message（从 messages 数组中获取最后一条消息的内容）
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        log.info("Received messages: {}", messages); // 添加这一行

        String message;
        if (messages != null && !messages.isEmpty()) {
            Map<String, Object> lastMessage = messages.get(messages.size() - 1); // 获取最后一条消息
            message = (String) lastMessage.get("content"); // 提取消息内容
            log.info("Extracted user message content: {}", message);

        } else {
            log.warn("Messages is empty or null");
            message = "";
        }

        // 提取 sessionId（假设从 variables 中获取）
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        ChatRequestVariables chatVars = objectMapper.convertValue(variables, ChatRequestVariables.class);
        String sessionId = chatVars.getSessionId();
        String userId = chatVars.getUid();

        // 查询历史记录 ✅ 添加这一行
        List<ConversationHistory> history = conversationService.getConversationHistoryBySessionId(sessionId);


        // 在提取完 message 后添加这一段
        List<Map<String, Object>> fullMessages = new ArrayList<>(history.size() + 1);

        for (ConversationHistory h : history) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", h.getRole());
            msg.put("content", h.getContent());
            fullMessages.add(msg);
        }

        // 添加当前用户消息
        Map<String, Object> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", message);
        fullMessages.add(currentMessage);

        // 默认模型类型（可以从请求中提取，也可以根据业务逻辑设定）
        String modelType = "memory";

        // 提取 fileId（假设从 variables 或其他字段传入）
        String fileId = (String) request.get("fileId");

        // 查询历史记录
        history = conversationService.getConversationHistoryBySessionId(sessionId);

        //AtomicReference<String> aiConversationIdRef = new AtomicReference<>("");

        String aiConversationId;
        if (!history.isEmpty()) {
            ConversationHistory lastMessage = history.get(history.size() - 1);
            if (lastMessage != null && lastMessage.getAiConversationId() != null) {
                aiConversationId = lastMessage.getAiConversationId();
            } else {
                aiConversationId = " ";
            }
        } else {
            aiConversationId = " ";
        }

        AtomicReference<String> fullAnswer = new AtomicReference<>("");

        log.info("Final message: {}", message);
        log.info("Full conversation history: {}", fullMessages);
        log.info("Session ID: {}", sessionId);
        log.info("User ID: {}", userId);
        log.info("File ID: {}", fileId);


        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.create(sink -> {

                    largeModelService.streamLargeModelResponse(message, fullMessages, aiConversationId, userId, fileId)
                            .subscribe(
                                    chunk -> {
                                        // ✅ 直接发送原始 chunk 内容
                                        sink.next(chunk);

                                        fullAnswer.updateAndGet(v -> v + chunk);
                                    },
                                    error -> {
                                        log.error("Streaming error", error);
                                        sink.error(error);
                                    },
                                    () -> {
                                        try {
                                            String answerStr = fullAnswer.get();

                                            // ✅ 发送 [DONE] 标志表示完成
                                            sink.next("[DONE]");

                                            // 保存对话历史...
                                            final ConversationHistory aiMessage = new ConversationHistory();
                                            aiMessage.setId(UUID.randomUUID().toString());
                                            aiMessage.setSessionId(sessionId);
                                            aiMessage.setRole("assistant");
                                            aiMessage.setContent(answerStr);
                                            aiMessage.setUserId(userId);
                                            aiMessage.setAiConversationId(aiConversationId);
                                            aiMessage.setCreateTime(java.time.LocalDateTime.now());

                                            if (fileId != null && !fileId.isEmpty()) {
                                                aiMessage.setFileId(fileId);
                                            }

                                            conversationService.saveConversation(aiMessage);

                                            sink.complete(); // 完成流
                                        } catch (Exception e) {
                                            log.error("Error saving AI response to DB", e);
                                            sink.complete();
                                        }
                                    }
                            );

                }));


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
}
