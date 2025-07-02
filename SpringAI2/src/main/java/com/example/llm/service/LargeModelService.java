package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
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
    //private static final String API_URL = "http://192.168.31.132:5001/v1/chat-messages";
    private static final String API_URL="http://cppmis-app.bjyhkx.com:3000/api/v1/chat/completions";

    private static final String API_KEY = "Bearer fastgpt-mlzFWJnROiDMDWt4axH5gD92vU1uFACAFADYi8mHu6kgV6p6WX3ZThvDS6";
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


    /**
     * 流式模式
     */
    private final WebClient webClient;

    public LargeModelService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
    }


    public Flux<String> streamLargeModelResponse(String message,  List<Map<String, Object>> fullMessages,
                                                 String aiConversationId, String userId, String fileId) {

        Map<String, Object> requestBody = buildRequestBody(message,fullMessages, aiConversationId, userId, fileId);


        log.info("Streaming to model with message: {}", message);
        log.info("Full messages sent to model: {}", fullMessages);


        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION, API_KEY)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);
    }

    private Map<String, Object> buildRequestBody(String query, List<Map<String, Object>> history,
                                                 String conversationId, String userId,
                                                 String fileId) {
        Map<String, Object> apiRequestBody = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        for (Map<String, Object> h : history) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", h.get("role"));     // 获取角色
            msg.put("content", h.get("content")); // 获取内容
            messages.add(msg);
        }


        apiRequestBody.put("chatId", conversationId); // 使用 conversationId 作为 chatId
        apiRequestBody.put("stream", true);
        apiRequestBody.put("detail", true);
        apiRequestBody.put("responseChatItemId", "my_responseChatItemId"); // 可根据需求动态生成

        Map<String, Object> variables = new HashMap<>();
        variables.put("uid", userId);
        variables.put("name", "用户"); // 可根据实际情况设置名称
        apiRequestBody.put("variables", variables);

        apiRequestBody.put("messages", messages);

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
