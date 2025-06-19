package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class LargeModelService {
    private static final String API_URL = "http://192.168.31.132:5001/v1/chat-messages";
    /**
     * 速理通AI智能助手密钥
     */
    //private static final String API_KEY = "Bearer app-2ALbJEuxLvPYaxKiHmWBQ0Ie";

    /**
     * slt OCR工作流密钥
     */
    //private static final String API_KEY = "Bearer app-xHC0zjFEdqcyeTGhZBoEOcEa";

    /**
     * 数据分析密钥
     */
    //private static final String API_KEY = "Bearer app-jr6BPoFeP7xZHoTTJTOXTkTH";

    /**
     * 记忆助手密钥
     */
    private static final String API_KEY = "Bearer app-4uxfoOAe7z77wwKDF5jHzhvs";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> askLargeModel(String query, List<ConversationHistory> history,
                                           String conversationId, String userId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // 构建消息历史
            List<Map<String, Object>> messages = new ArrayList<>();
            history.forEach(h -> {
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", h.getRole());
                msg.put("content", h.getContent());
                messages.add(msg);
            });

            // 构建请求体
            Map<String, Object> apiRequestBody = new HashMap<>();

            if (messages.isEmpty()) {
                // 对于新对话，发送空的inputs对象
                apiRequestBody.put("inputs", new HashMap<>());
            } else {
                // 对于现有对话，发送包含消息历史的inputs对象
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("messages", messages);
                apiRequestBody.put("inputs", inputs);
            }

            apiRequestBody.put("query", query);
            apiRequestBody.put("response_mode", "blocking");
            apiRequestBody.put("conversation_id", conversationId);
            apiRequestBody.put("user", userId);

            // 发送请求
            HttpPost post = new HttpPost(API_URL);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            post.setHeader(HttpHeaders.AUTHORIZATION, API_KEY);
            post.setHeader("X-Request-Id", UUID.randomUUID().toString());

            String json = objectMapper.writeValueAsString(apiRequestBody);
            log.info("Request to AI: {}", json);
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("AI Response: {}", responseJson);

                JsonNode responseNode = objectMapper.readTree(responseJson);

                if (responseNode.has("error")) {
                    String errorMessage = responseNode.get("error").get("message").asText();
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", errorMessage);
                    return errorMap;
                }

                // 构建响应对象
                Map<String, Object> responseMap = new HashMap<>();
                if (responseNode.has("answer")) {
                    String answer = responseNode.get("answer").asText();
                    // 移除末尾的 "//"
                    if (answer.endsWith("//")) {
                        answer = answer.substring(0, answer.length() - 2);
                    }
                    responseMap.put("answer", answer);
                }

                if (responseNode.has("metadata")) {
                    responseMap.put("metadata", responseNode.get("metadata"));
                }

                if (responseNode.has("conversation_id")) {
                    responseMap.put("conversation_id", responseNode.get("conversation_id").asText());
                }

                if (responseNode.has("message_id")) {
                    responseMap.put("message_id", responseNode.get("message_id").asText());
                }

                return responseMap;
            }
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "调用AI服务时发生错误: " + e.getMessage());
            return errorMap;
        }
    }


    /**
     * 流式模式
     */
    private final WebClient webClient;

    public LargeModelService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
    }

    public Flux<String> streamLargeModelResponse(String query, List<ConversationHistory> history, String conversationId, String userId) {
        Map<String, Object> requestBody = buildRequestBody(query, history, conversationId, userId);

        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION, API_KEY)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class); // 按照实际返回类型调整
    }

    private Map<String, Object> buildRequestBody(String query, List<ConversationHistory> history, String conversationId, String userId) {
        Map<String, Object> apiRequestBody = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        history.forEach(h -> {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", h.getRole());
            msg.put("content", h.getContent());
            messages.add(msg);
        });

        if (messages.isEmpty()) {
            apiRequestBody.put("inputs", new HashMap<>());
        } else {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("messages", messages);
            apiRequestBody.put("inputs", inputs);
        }

        apiRequestBody.put("query", query);
        apiRequestBody.put("response_mode", "streaming"); // 启用流式响应
        apiRequestBody.put("conversation_id", conversationId);
        apiRequestBody.put("user", userId);

        return apiRequestBody;
    }
}
