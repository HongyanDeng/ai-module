package com.example.llm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LargeModelController {
    //大模型接口地址
    private static final String API_URL = "http://192.168.31.132:5001/v1/chat-messages";
    //大模型接口密钥
    private static final String API_KEY = "Bearer app-2ALbJEuxLvPYaxKiHmWBQ0Ie";

    //对话接口路径
    @PostMapping("/ask")
    //使用通配符？表示响应体的数据类型,返回类型有多种
    public ResponseEntity<?> askLargeModel(@RequestBody Map<String, Object> requestBody) {
        //创建HTTP客户端实例
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            //创建HttpPost请求实例，设置目标API_URL
            HttpPost post = new HttpPost(API_URL);
            //指定请求头格式为json
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            //传入API密钥
            post.setHeader(HttpHeaders.AUTHORIZATION, API_KEY);

            ObjectMapper mapper = new ObjectMapper();
            //将请求体序列化为json字符串
            String json = mapper.writeValueAsString(requestBody);
            //通过StringEntity将json字符串封装成HTTP请求体
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            //发送HTTP请求，获取响应
            try (CloseableHttpResponse response = client.execute(post)) {
                //获取响应体并转换为字符串
                String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                //将响应json字符串反序列化为JsonNode并返回给客户端
                return ResponseEntity.ok().body(mapper.readTree(responseJson));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
