

# ai-module

# **一、前期准备工作**

## **1.安装软件**

1.1 Intellij IDEA，2024.3.5版本

1.2 Visual Studio Code

## **2.安装node.js**

### 2.1 macOS/Linux安装nvm:

方式一：使用官方安装脚本

打开终端，输入以下命令：

```
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```

方式二：使用wget

打开终端，输入以下命令：

```
wget -qO- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```

安装后重启终端，使其生效

在终端中输入以下内容，验证安装是否成功：

```
nvm --version
```



### 2.2 Windows安装nvm

打开github项目地址

```
https://github.com/coreybutler/nvm-windows
```

点击右侧的Releases

![v](images\nvm.png)



下载带安装器的.exe文件（如nvm-setup.exe)，并运行安装

![v](images\nvm2.png)

安装完成后，在终端中输入以下内容，验证是否安装成功：

```
nvm version
```



### 2.3 安装node.js

选择20.17.0版本（至少要18.16.0以上）

打开终端，输入以下代码：

```
nvm install 20.17.0
nvm use 20.17.0
```



# 二、后端接入

## 1.新建maven项目

jdk版本选择17

![ave](images\maven.png)



## 2.配置pom.xml

```java
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>
```

```
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>
</dependencies>
```



## 3.编写大模型controller文件

LargeModelController.java

```
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
```



## 4.配置跨域访问

```
package com.example.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")  //使用通配符*表示允许所有源访问
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```



## 5.编写启动项

```
package com.example.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("🚀 服务已启动，访问地址：http://localhost:8080");
    }
}
```



# 三、前端接入

## 1.新建Vue项目

node.js版本选择20.17.0

![u](images\vue.png)

在终端中运行以下代码下载必要包：

```
npm install 
```



## 2.配置vue.config.js文件

```
module.exports = {
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080', // 后端 Spring Boot 地址
                changeOrigin: true
            }
        }
    }
};
```



## 3.新建大模型问答组件

新建文件frontend/src/components/AskModel.vue

### 3.1 页面主体

```
<template>
  <div class="ask-model">
    <h2>智能问答</h2>
    <input v-model="question" placeholder="请输入你的问题" @keyup.enter="askModel" />
    <button @click="askModel">提问</button>
    <div v-if="loading">正在生成回答...</div>
    <div v-if="answer">
      <h3>回答：</h3>
      <pre>{{ answer }}</pre>
    </div>
  </div>
</template>
```

### 3.2 script部分

```
<script>
import axios from 'axios';

export default {
  name: 'AskModel',
  data() {
    return {
      question: '',
      answer: '',
      loading: false
    };
  },
  methods: {
    async askModel() {
      if (!this.question) return;
      this.loading = true;
      this.answer = '';
      try {
        //  后端大模型问答接口
        const response = await axios.post('http://localhost:8080/api/llm/ask', {
         //  请求参数
          inputs: {},
          query: this.question,
          response_mode: 'blocking',
          conversation_id: '',
          user: 'abc-123',
          answer: ''
        });
        //  处理返回结果
        this.answer = response.data.answer || JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.answer = '请求失败: ' + error.message;
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
```



## 4.在页面中引入大模型问答组件

```
<template>
  <div id="app">
    <AskModel />
  </div>
</template>

<script>
import AskModel from './components/AskModel.vue';
export default {
  components: { AskModel }
};
</script>
```



# 四、启动项目

## 1.启动后端Application启动项

![un](images\run1.png)

## 2.启动前端

![un](images\run2.png)

## 3.成功访问页面

![un](images\run3.png)