package com.example.llm.controller;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.service.ConversationService;
import com.example.llm.service.LargeModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/llm")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LargeModelController {
    private final LargeModelService largeModelService;
    private final ConversationService conversationService;

    @PostMapping("/ask")
    public ResponseEntity<?> askLargeModel(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            String frontendSessionId = (String) request.get("sessionId");
            String userId = (String) request.get("userId");

            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "消息内容不能为空");
                return ResponseEntity.badRequest().body(errorMap);
            }

            // 确定用于数据库历史记录的会话ID (来自前端或新的时间戳式ID)
            String currentSessionId = (frontendSessionId != null && !frontendSessionId.isEmpty()) ? 
                frontendSessionId : "session-" + System.currentTimeMillis();
            
            // 确定最终的用户ID
            String finalUserId = (userId != null && !userId.isEmpty()) ? 
                userId : "default-user";

            // 使用 currentSessionId 获取历史记录
            List<ConversationHistory> history = conversationService.getConversationHistoryBySessionId(currentSessionId);
            
            // 总是发送空字符串作为 conversation_id 给 AI 服务，让AI服务自己管理。
            // 这样处理是基于 ai.txt 中请求示例的 conversation_id 为空字符串的假设。
            String conversationIdForAI = ""; 

            // 保存用户消息
            ConversationHistory userMessage = new ConversationHistory();
            userMessage.setConversationId(UUID.randomUUID().toString()); // 为本次交互生成一个新的UUID，作为数据库的 conversationId
            userMessage.setSessionId(currentSessionId); // 保存前端的会话ID
            userMessage.setRole("user");
            userMessage.setContent(message);
            userMessage.setUserId(finalUserId);
            conversationService.saveConversation(userMessage);

            // 调用AI服务
            Map<String, Object> response = largeModelService.askLargeModel(
                message, history, conversationIdForAI, finalUserId);

            if (response.containsKey("error")) {
                // 如果AI返回错误，也保存错误信息作为AI的响应，便于调试
                ConversationHistory errorMessage = new ConversationHistory();
                errorMessage.setConversationId(UUID.randomUUID().toString());
                errorMessage.setSessionId(currentSessionId);
                errorMessage.setRole("assistant_error");
                errorMessage.setContent((String) response.get("error"));
                errorMessage.setUserId(finalUserId);
                conversationService.saveConversation(errorMessage);
                return ResponseEntity.badRequest().body(response);
            }

            // 保存AI响应
            if (response.containsKey("answer")) {
                ConversationHistory aiMessage = new ConversationHistory();
                aiMessage.setConversationId(UUID.randomUUID().toString()); // 为本次交互生成一个新的UUID
                aiMessage.setSessionId(currentSessionId); // 保存前端的会话ID
                aiMessage.setRole("assistant");
                aiMessage.setContent((String) response.get("answer"));
                aiMessage.setUserId(finalUserId);
                // 从AI响应中获取AI的 conversation_id 和 message_id
                if (response.containsKey("conversation_id")) {
                    aiMessage.setAiConversationId((String) response.get("conversation_id"));
                }
                if (response.containsKey("message_id")) {
                    aiMessage.setAiMessageId((String) response.get("message_id"));
                }
                conversationService.saveConversation(aiMessage);
            }

            // 返回响应给前端，包含 currentSessionId 以便前端跟踪
            response.put("sessionId", currentSessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing request", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "处理请求时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorMap);
        }
    }
}
