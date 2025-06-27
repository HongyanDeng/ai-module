package com.example.llm.controller;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.service.ConversationService;
import com.example.llm.service.LargeModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * 获取流式 Large Model 响应，并在完成后保存 AI 回答到数据库
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public ResponseEntity<Flux<String>> askLargeModelStream(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        String sessionId = (String) request.get("sessionId");
        String userId = (String) request.get("userId");
        String modelType = (String) request.get("modelType");
        String fileId = (String) request.get("fileId");

        List<ConversationHistory> history = conversationService.getConversationHistoryBySessionId(sessionId);

        AtomicReference<String> aiConversationIdRef = new AtomicReference<>("");

        if (!history.isEmpty()) {
            ConversationHistory lastMessage = history.get(history.size() - 1);
            if (lastMessage != null && lastMessage.getAiConversationId() != null) {
                aiConversationIdRef.set(lastMessage.getAiConversationId());
            }
        }

        AtomicReference<String> fullAnswer = new AtomicReference<>("");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(Flux.create(sink -> {

                    largeModelService.streamLargeModelResponse(message, history, aiConversationIdRef.get(), userId,modelType,fileId)
                            .subscribe(
                                    chunk -> {
                                        sink.next(chunk); // 发送给前端
                                        fullAnswer.updateAndGet(v -> v + chunk); // 累积完整回答
                                    },
                                    error -> {
                                        log.error("Streaming error", error);
                                        sink.error(error);
                                    },
                                    () -> {
                                        try {
                                            String answerStr = fullAnswer.get();

                                            // 解析 JSON 获取 conversation_id 和 message_id
                                            JsonNode jsonNode = objectMapper.readTree(answerStr);


                                            String conversationId = jsonNode.has("conversation_id") ?
                                                    jsonNode.get("conversation_id").asText() : UUID.randomUUID().toString();

                                            String messageId = jsonNode.has("message_id") ?
                                                    jsonNode.get("message_id").asText() : null;

                                            /**
                                             * 检查是否真的存入数据库
                                             */
                                            log.info("准备保存 AI 回答：{}", answerStr);
                                            log.info("回答长度：{}", answerStr.length());
                                            log.info("session_id: {}, conversation_id: {},file_id{}", sessionId, conversationId,fileId);

                                            // 构造实体类并保存
                                            final ConversationHistory aiMessage = new ConversationHistory();

                                            aiMessage.setId(UUID.randomUUID().toString());
                                            aiMessage.setSessionId(sessionId);
                                            aiMessage.setRole("assistant");
                                            aiMessage.setContent(answerStr); // 或只保存纯文本部分
                                            aiMessage.setUserId(userId);

                                            aiMessage.setAiConversationId(conversationId);
                                            aiMessage.setAiMessageId(messageId);
                                            aiMessage.setCreateTime(java.time.LocalDateTime.now());

                                            // 如果请求中包含 fileId，则保存到 content 字段或新建字段
                                            if (fileId != null && !fileId.isEmpty()) {
                                                aiMessage.setFileId(fileId);
                                            }

                                            log.info("开始保存到数据库...");
                                            conversationService.saveConversation(aiMessage);
                                            log.info("保存完成！");

                                            sink.complete(); // 完成流
                                        } catch (Exception e) {
                                            log.error("Error saving AI response to DB", e);
                                            sink.complete(); // 即使失败也继续完成
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
