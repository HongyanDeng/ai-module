package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.web.multipart.MultipartFile;
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
    //private static final String API_KEY = "Bearer app-4uxfoOAe7z77wwKDF5jHzhvs";



    private String getApiKeyByModelType(String modelType) {
        switch (modelType) {
            case "ai":
                return "Bearer app-2ALbJEuxLvPYaxKiHmWBQ0Ie"; // AI助手密钥
            case "data":
                return "Bearer app-jr6BPoFeP7xZHoTTJTOXTkTH"; // 数据分析密钥
            case "ocr":
                return "Bearer app-xHC0zjFEdqcyeTGhZBoEOcEa"; // OCR工作流密钥
            case "memory":
                return "Bearer app-4uxfoOAe7z77wwKDF5jHzhvs"; // 记忆助手密钥
            default:
                throw new IllegalArgumentException("未知的大模型类型: " + modelType);
        }
    }

    /**
     * 流式模式
     */
    private final WebClient webClient;

    public LargeModelService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
    }

    public Flux<String> streamLargeModelResponse(String query, List<ConversationHistory> history,
                                                 String conversationId,String userId,
                                                 String modelType,String fileId) {
        String API_KEY = getApiKeyByModelType(modelType);

        Map<String, Object> requestBody = buildRequestBody(query, history, conversationId, userId, modelType, fileId);

        log.info("当前模型：{}", modelType);
        log.info("发送流式请求到：{}", requestBody);
        log.info("流式请求参数 -> message: {}, conversationId: {}, userId: {}",
                query, conversationId, userId);

        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION, API_KEY)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class); // 按照实际返回类型调整
    }

    private Map<String, Object> buildRequestBody(String query, List<ConversationHistory> history,
                                                 String conversationId,String userId,
                                                 String modelType,String fileId) {
        Map<String, Object> apiRequestBody = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        for (ConversationHistory h : history) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", h.getRole());
            msg.put("content", h.getContent());
            messages.add(msg);
        }

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
        apiRequestBody.put("model_type", modelType);

        // ✅ 如果有 fileId，就加进去
        if (fileId != null && !fileId.isEmpty()) {
            List<Map<String, Object>> files = new ArrayList<>();
            Map<String, Object> file = new HashMap<>();
            file.put("type", "image");
            file.put("transfer_method", "local_file");
            file.put("upload_file_id", fileId);
            files.add(file);
            apiRequestBody.put("files", files);
        }
        log.info("请求参数 -> {}", apiRequestBody);
        return apiRequestBody;
    }


    /**
     * 实现上传文件方法
     */
    public Map<String, Object> uploadFile(MultipartFile file, String user) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost("http://192.168.31.132:5001/v1/files/upload");

            String API_KEY = "Bearer app-4uxfoOAe7z77wwKDF5jHzhvs";//记忆助手密钥
            // 设置授权头
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, API_KEY);

            // 获取原始文件名并进行 URL 编码
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名为空");
            }

            // 对文件名进行 URL 编码，避免非法字符
            String safeFileName = URLEncoder.encode(originalFilename, "UTF-8");

            // 构建表单数据
            org.apache.hc.core5.http.ContentType contentType = org.apache.hc.core5.http.ContentType.create(file.getContentType());

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file.getInputStream(), contentType, safeFileName);
            builder.addTextBody("user", user);

            httpPost.setEntity(builder.build());

            // 发送请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

}
