

# AI-module

# 一、前期准备工作

阅读完成README.md文件



# 二、后端代码实现

## 2.1 controller层

**LargeModelController.java**

```java
package com.example.llm.controller;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.service.ConversationService;
import com.example.llm.service.LargeModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
```



## 2.2 entity层

**ConversationHistory.java**

```java
package com.example.llm.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "conversation_history")
@EntityListeners(AuditingEntityListener.class)
public class ConversationHistory {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "VARCHAR(255)", nullable = false)
    private String id;

    @Column(name = "conversation_id") 
    private String conversationId;

    @Column(name = "ai_conversation_id") 
    private String aiConversationId;

    @Column(name = "ai_message_id") 
    private String aiMessageId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "role")
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_device")
    private String userDevice;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_name")
    private String modelName;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

  
}
```



**数据表结构：conversation_history**

```mysql
create table conversation_history
(
    id                 varchar(255)                        not null
        primary key,
    conversation_id    varchar(100) charset utf8           null,
    user_id            varchar(100) charset utf8           null,
    role               varchar(100) charset utf8           null,
    content            text charset utf8                   null,
    created_at         timestamp default CURRENT_TIMESTAMP null,
    ai_conversation_id varchar(255) charset utf8           null,
    ai_message_id      varchar(255) charset utf8           null,
    create_time        datetime(6)                         null,
    ip_address         varchar(255) charset utf8           null,
    model_name         varchar(255) charset utf8           null,
    session_id         varchar(255) charset utf8           null,
    token_count        int                                 null,
    user_device        varchar(255) charset utf8           null
)
    collate = utf8_unicode_ci;
```



## 2.3 service层

**LargeModelService.java**

```java
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
    private static final String API_KEY = "Bearer app-jr6BPoFeP7xZHoTTJTOXTkTH";

    /**
     * 记忆助手密钥
     */
    //private static final String API_KEY = "Bearer app-4uxfoOAe7z77wwKDF5jHzhvs";

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
}
```



**ConversationService.java**

```java
package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import java.util.List;

public interface ConversationService {
    void saveConversation(ConversationHistory conversation);
    List<ConversationHistory> getConversationHistory(String sessionId);
    List<ConversationHistory> getConversationHistoryBySessionId(String sessionId);
} 
```



**ConversationServiceImpl.java**

```java
package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.repository.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public List<ConversationHistory> getConversationHistory(String conversationId) {
        return conversationHistoryRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
    }

    @Override
    public List<ConversationHistory> getConversationHistoryBySessionId(String sessionId) {
        return conversationHistoryRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    @Override
    public void saveConversation(ConversationHistory conversationHistory) {
        conversationHistoryRepository.save(conversationHistory);
    }
} 
```



## **2.4 repository层**

**ConversationHistoryDao.java**

```Java
package com.example.llm.repository;

import com.example.llm.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, String> {
    List<ConversationHistory> findByConversationIdOrderByCreateTimeAsc(String conversationId);
    List<ConversationHistory> findBySessionIdOrderByCreateTimeAsc(String sessionId);
} 
```



## 2.5 application.properties

```java
server.port=8080
spring.application.name=llm-gateway

spring.datasource.url=jdbc:mysql://localhost:3306/ai01?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
```



## 2.6 启动类

**Application.java**

```java
package com.example.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.llm.entity")
@EnableJpaRepositories("com.example.llm.repository")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("🚀 服务已启动，访问地址：http://localhost:8080");
    }
}
```



# 三、前端代码实现

App.vue和vue.config.js与README1.md相同

AskModel.vue

```vue
<template>
  <div class="chat-container">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h2>速理通智能问答</h2>
        <button class="new-chat-btn" @click="createNewChat">
          <span>+</span> 新建对话
        </button>
      </div>

      <div class="conversation-list">
        <div v-for="(conversation, index) in conversations" :key="conversation.id"
          :class="['conversation-item', { active: currentConversationId === conversation.id }]"
          @click="switchConversation(conversation.id)">
          <div class="conversation-info">
            <div class="conversation-title">
              {{ conversation.title || '新对话' }}
              {{ formatTime(conversation.createdAt) }}

              <button class="delete-btn" @click.stop="deleteConversation(conversation.id)"
                v-if="conversations.length > 1" style="margin-left: 15px;">
                🗑️
              </button>

            </div>
            <!--
            <div class="conversation-time">{{ formatTime(conversation.createdAt) }}</div>
          -->
          </div>


        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-page">
      <div class="chat-content">
        <div v-for="(msg, idx) in currentMessages" :key="idx" :class="['chat', msg.role]">
          <div v-if="msg.role === 'ai'" class="ai-answer">

            <span class="ai-text">{{ msg.text }}</span>
          </div>
          <div v-if="msg.role === 'user'" class="user-question">

            <span class="user-text">{{ msg.text }}</span>
          </div>
        </div>
        <div v-if="loading" class="chat-ai">
          <div class="loading-answer">
            <span></span>
            <span class="long-ai-answer">正在生成回答...</span>
          </div>
        </div>
      </div>
      <div class="chat-input-bar">
        <textarea v-model="question" placeholder="询问任何问题" @keyup.enter="askModel"></textarea>
        <div class="send-button">
          <button class="send-file" @click="sendFile">＋</button>
          <button class="ask-model" @click="askModel">↑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AskModel',
  data() {
    return {
      question: '',
      loading: false,
      sessionId: '',
      conversations: [],
      currentConversationId: null,
      currentMessages: [
        { role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }
      ]
    };
  },
  methods: {
    async askModel() {
      if (!this.question) return;
      const userMsg = { role: 'user', text: this.question };
      this.currentMessages.push(userMsg);
      this.loading = true;
      const q = this.question;
      this.question = '';
      try {
        const response = await axios.post('http://localhost:8080/api/llm/ask', {
          message: q,
          sessionId: this.sessionId || '',
          //sessionId: this.sessionId,
          userId: 'user-' + Date.now()
          //userId:this.userId,
        });

        let aiResponse = '';
        if (response.data && response.data.answer) {
          aiResponse = response.data.answer;
          // 处理换行符
          aiResponse = aiResponse.replace(/\\n/g, '\n');
          // 移除末尾的 "//"
          if (aiResponse.endsWith("//")) {
            aiResponse = aiResponse.substring(0, aiResponse.length() - 2);
          }
        } else if (response.data && response.data.error) {
          aiResponse = '错误: ' + response.data.error;
        } else {
          aiResponse = '抱歉，我无法理解这个回答。';
        }

        this.currentMessages.push({ role: 'ai', text: aiResponse });

        // 更新当前对话的标题（使用第一条用户消息）
        this.updateConversationTitle(q);
        await this.$nextTick();
        this.scrollToBottom();
      } catch (error) {
        console.error('Error:', error);
        let errorMessage = '请求失败';
        if (error.response) {
          if (error.response.data && error.response.data.error) {
            errorMessage = error.response.data.error;
          } else if (error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
          } else {
            errorMessage = error.response.data || error.response.statusText;
          }
        } else if (error.message) {
          errorMessage = error.message;
        }
        this.currentMessages.push({ role: 'ai', text: errorMessage });
      } finally {
        this.loading = false;
      }
    },
    scrollToBottom() {
      const container = this.$refs.messagesContainer || document.querySelector('.chat-content');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },
    createNewChat() {
      const newConversation = {
        id: 'conv-' + Date.now(),
        title: '新对话',
        messages: [{ role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }],
        createdAt: new Date()
      };

      this.conversations.push(newConversation);
      this.switchConversation(newConversation.id);
    },

    switchConversation(conversationId) {
      this.currentConversationId = conversationId;
      const conversation = this.conversations.find(c => c.id === conversationId);
      if (conversation) {
        this.currentMessages = [...conversation.messages];
        this.sessionId = 'session-' + conversationId;
      }
    },

    deleteConversation(conversationId) {
      if (this.conversations.length <= 1) {
        alert('至少需要保留一个对话');
        return;
      }

      const index = this.conversations.findIndex(c => c.id === conversationId);
      if (index > -1) {
        this.conversations.splice(index, 1);

        // 如果删除的是当前对话，切换到第一个对话
        if (this.currentConversationId === conversationId) {
          this.switchConversation(this.conversations[0].id);
        }
      }
    },

    updateConversationTitle(firstMessage) {
      const conversation = this.conversations.find(c => c.id === this.currentConversationId);
      if (conversation && !conversation.title || conversation.title === '新对话') {
        // 使用第一条用户消息的前20个字符作为标题
        conversation.title = firstMessage.length > 20 ? firstMessage.substring(0, 10) + '...' : firstMessage;
      }
    },

    formatTime(timestamp) {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;

      if (diff < 60000) { // 1分钟内
        return '刚刚';
      } else if (diff < 3600000) { // 1小时内
        return Math.floor(diff / 60000) + '分钟前';
      } else if (diff < 86400000) { // 1天内
        return Math.floor(diff / 3600000) + '小时前';
      } else {
        return date.toLocaleDateString();
      }
    }
  },
  created() {
    // 初始化第一个对话
    const initialConversation = {
      id: 'conv-' + Date.now(),
      title: '新对话',
      messages: [{ role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }],
      createdAt: new Date()
    };

    this.conversations.push(initialConversation);
    this.currentConversationId = initialConversation.id;
    this.sessionId = 'session-' + this.currentConversationId;
  },
  watch: {
    currentMessages: {
      handler(newMessages) {
        // 同步当前消息到对话记录
        const conversation = this.conversations.find(c => c.id === this.currentConversationId);
        if (conversation) {
          conversation.messages = [...newMessages];
        }
      },
      deep: true
    }
  }
};
</script>

<style scoped>
@keyframes dot-animation {

  0%,
  100% {
    opacity: 0.3
  }

  50% {
    opacity: 1
  }
}

html,
body {
  height: 80%;
  margin: 0;
  padding: 0;
  background: #f4f6fa;
}

.chat-container {
  display: flex;
  height: 100%;
}

.sidebar {
  /**采用绝对定位 */
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 260px;
  background: #d6d6d6;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  /*background: #f7f7f8;*/
  /*border-bottom: 1px solid #e6e6e6;*/
}

.sidebar-header h2 {
  margin: 0 0 12px 10px;
  font-size: 26px;
  font-family: 宋体;
  color: #000000;
  font-weight: 500;
}

.new-chat-btn {
  width: 100%;
  padding: 8px 12px;
  background: #ffffff;
  color: #000000;
  border: 1px solid #e6e6e6;
  border-radius: 15px;
  font-size: 14px;
  font-weight: normal;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.new-chat-btn:hover {
  background: #f3f3f3;
  transform: none;
  /**增加深色阴影 */
  box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1);
}

.chat-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  padding: 0;
  margin-left: 90px;
}

.chat-content {
  flex: 1;
  width: 1000px;
  margin-left: 140px;
  padding-left: 10px;
  background-color: rgb(255, 255, 255);
  margin-bottom: 200px;
}

.chat-input-bar {
  height: 160px;
  position: fixed;
  width: 1000px;
  left: 60%;
  transform: translateX(-50%);
  bottom: 3%;
  padding: 20px 20px;
  background: #ffffff;
  border-top: 1px solid #e6e6e6;
  border-radius: 30px;
  box-shadow: 5px 5px 5px 5px rgba(0.1, 0.1, 0.1, 0.1);
}

.chat-input-bar input {
  width: 765px;
  padding: 5px 10px;
  /* 顶部和底部增加5px内边距，左右增加10px内边距 */
  height: 40px;
  /* 设置内容区域高度为40px，加上5px上下内边距，总高度为50px */
  line-height: 1.2;
  /* 设置行高为正常值，使其与字体大小匹配，避免垂直居中 */
  vertical-align: top;
  /* 明确将内容垂直向上对齐 */
  border: 0px solid #e6e6e6;
  border-radius: 6px;
  background: #ffffff;
  margin-left: 0px;
  margin-top: 0;
  transition: box-shadow 0.3s ease;
  /* 添加过渡效果 */
}

.chat-input-bar input:focus {
  box-shadow: 0 0 0px rgba(79, 140, 255, 0.5);
  /* 点击时的蓝色阴影效果 */
  outline: none;
  /* 移除默认的outline样式 */
}

.conversation-item {
  margin-left: 15px;
  margin-right: 15px;
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 12px;
  color: #000000;
  font-size: 16px;
}

/**键入对话颜色 */
.conversation-item:hover {
  background: #ffffff;
  transform: none;
}

.conversation-item.active {
  /*background: #1b1b82;*/
  border-color: transparent;
  box-shadow: none;
}

.conversation-item.active,
.new-chat-btn {
  /*background: #b51313;*/
  box-shadow: none;
}


/**用户提问框 */
.user-question {
  /**采用flex布局实现右对齐 */
  display: flex;
  justify-content: flex-end;
  max-width: 100%;
  margin-bottom: 16px;
  margin-right: 10px;
}

.user-question>span {
  background-color: rgb(226, 226, 226);
  border-radius: 30px 30px 0 30px;
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 80%;
  min-width: 120px;
  word-break: break-word;
  transition: all 0.3s ease;
  color: #000000;
}

/**大模型回答框 */
.ai-answer {
  /**采用flex布局实现左对齐 */
  display: flex;
  justify-content: flex-start;
  max-width: 100%;
  margin-bottom: 16px;
  margin-left: 0px;
  color: #000000;
}

.ai-answer>span {
  margin-top: 10px;
  background-color: rgb(226, 226, 226);
  border-radius: 25px 25px 25px 0;
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  max-width: 80%;
  min-width: 120px;
  word-break: break-word;
  transition: all 0.3s ease;
}

.send-button {
  margin-top: 5px;
  margin-left: 800px;
}

.send-file {
  margin-left: 10px;
  height: 50px;
  font-size: 25px;
  width: 50px;
  background-color: #000000;
  border-radius: 15px;
  border: white;
  color: #ffffff;
}


.ask-model {
  margin-left: 10px;
  height: 50px;
  font-size: 25px;
  width: 50px;
  background-color: #000000;
  border-radius: 15px;
  border: white;
  color: #ffffff;
}

/**提问框 */
.chat-input-bar textarea {
  width: 960px;
  padding: 10px;
  height: 60px; /* 固定高度 */
  line-height: 1.5;
  border: 0px solid #e6e6e6;
  border-radius: 6px;
  background: #9c2323;
  margin-left: 0;
  margin-top: 0;
  resize: none; /* 禁止手动调整大小 */
  transition: box-shadow 0.3s ease;
  font-size: 16px;
}

.chat-input-bar textarea:focus {
  outline: none;
  box-shadow: 0 0 5px rgba(255, 255, 255, 1);
}


</style>
```