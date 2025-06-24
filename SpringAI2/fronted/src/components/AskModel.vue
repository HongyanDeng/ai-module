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

        <div class="model-selector">
          <button type="primary" plain @click="switchModel('ai')" style="background-color:#e6f7ff ">
            <span class="model-icon"></span>
            AI助手
          </button>
          <button type="primary" plain @click="switchModel('data')" style="background-color:#f0fff0 ">
            <span class="model-icon"></span>
            数据分析
          </button>
          <button type="primary" plain @click="switchModel('ocr')" style="background-color: #fff8e6">
            <span class="model-icon"></span>
            OCR工作流
          </button>
          <button type="primary" plain @click="switchModel('memory')" style="background-color: #ffe6f0">
            <span class="model-icon"></span>
            记忆助手
          </button>
        </div>


      <div class="conversation-list"  ref="conversationList" @scroll="handleSidebarScroll">
        <div  v-for="(conversation, index) in conversations" :key="conversation.id"
              :class="['conversation-item',{ active: currentConversationId === conversation.id },
              conversation.modelType ? 'model-type-' + conversation.modelType : '']"
              :style="{ backgroundColor: modelColorMap[conversation.modelType] }"
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
          </div>


        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-page">
      <div class="chat-content">
        <div v-for="(msg, idx) in currentMessages" :key="idx" :class="['chat', msg.role]">
          <div v-if="msg.role === 'ai'" class="ai-answer">
            <div class="ai-text" v-html="renderMarkdown(msg.text).__html"></div>

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
          <input type="file" @change="sendFile" />

          <!--
          <button class="send-file" @click="sendFile">＋</button>
          -->

          <button class="ask-model" @click="askModel">⬆</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as marked from 'marked';

import DOMPurify from 'dompurify';

import {mangle} from 'marked-mangle';

marked.use(mangle());


export default {
  name: 'AskModel',
  data() {
    return {
      question: '',
      loading: false,
      sessionId: '',
      conversations: [],
      conversationId: '',
      currentConversationId: null,
      userId: null,
      currentModel: 'memory', // 默认模型（可选值：ai, data, ocr, memory）
      modelColorMap: {
        ai: '#e6f7ff',
        data: '#f0fff0',
        ocr: '#fff8e6',
        memory: '#ffe6f0'
      },
      sidebarScrollTop: 0,
      autoScrollEnabled: true,
      fileID:null,//文件 ID
      currentMessages: [
        { role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }
      ]
    };
  },
  methods: {
    renderMarkdown(text) {
      return { __html: DOMPurify.sanitize(marked.parse(text)) };
    },
    /**
     * 切换模型
     */
    switchModel(modelType) {
      this.currentModel = modelType;
      this.createNewChat(); //  切换模型时自动新建对话
    },

    handleSidebarScroll(event) {
      const container = event.target;
      this.sidebarScrollTop = container.scrollTop;

      // 判断是否已滚动到底部
      const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 5;
      this.autoScrollEnabled = isAtBottom;
    },

    scrollToBottomOfSidebar() {
      this.$nextTick(() => {
        const listContainer = this.$refs.conversationList;
        if (!listContainer) return;

        if (this.autoScrollEnabled) {
          listContainer.scrollTop = listContainer.scrollHeight;
        } else {
          // 如果用户没有在底部，则恢复之前的滚动位置
          listContainer.scrollTop = this.sidebarScrollTop;
        }
      });
    },

    /*上传文件到模型*/
    async sendFile(event) {
      const file = event.target.files[0];
      if (!file) return;

      const formData = new FormData();
      formData.append('file', file);
      formData.append('user', this.userId);

      try {
        const response = await fetch('http://localhost:8080/api/llm/upload', {
          method: 'POST',
          body: formData,
        });

        if (!response.ok) {
          throw new Error('Upload failed');
        }

        const result = await response.json();
        console.log('File uploaded:', result);

        // 将文件 ID 保存到组件数据中
        this.fileId = result.id;
        alert('文件上传成功！可以开始提问');

        // 可选：将文件信息发送给 AI 模型
        /*
        this.currentMessages.push({
          role: 'user',
          text: `文件已上传: ${result.name} (ID: ${result.id})`,
        });*/
        // 可选：显示一条提示消息
        this.currentMessages.push({
          role: 'ai',
          text: `文件已上传，ID: ${result.id}。你可以开始提问了。`
        });

      } catch (error) {
        console.error('Error uploading file:', error);
        this.currentMessages.push({
          role: 'ai',
          text: '文件上传失败: ' + error.message,
        });
      }
    },


    /*模型问答*/
    async askModel() {
      if (!this.question) return;

      const userMsg = { role: 'user', text: this.question };
      this.currentMessages.push(userMsg);
      this.loading = true;
      const q = this.question.trim();
      this.question = '';

      try {
        const response = await fetch('http://localhost:8080/api/llm/ask-stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            message: q,
            sessionId: this.sessionId || '',
            userId: this.userId || '', // 使用固定 userId
            conversationId: this.conversationId || '' ,
            modelType: this.currentModel,
            fileId: this.fileId || '', // 包含文件 ID（如果有）
          })
        });
        console.log('当前 fileId:', this.fileId);

        if (!response.ok) {
          throw new Error('Network response was not ok');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let aiResponse = '';

        // 创建一个空的 AI 消息占位符
        const aiMessageIndex = this.currentMessages.length;
        this.currentMessages.push({ role: 'ai', text: '' });

        let buffer = '';
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value, { stream: true });
          buffer += chunk;

          // 提取所有可能的 JSON 对象
          const jsons = [];
          let startIdx = buffer.indexOf('{');
          let endIdx = -1;
          let depth = 0;

          while (startIdx !== -1) {
            for (let i = startIdx; i < buffer.length; i++) {
              if (buffer[i] === '{') depth++;
              else if (buffer[i] === '}') depth--;

              if (depth === 0) {
                endIdx = i;
                break;
              }
            }

            if (depth === 0 && endIdx > startIdx) {
              const potentialJson = buffer.slice(startIdx, endIdx + 1);
              try {
                const parsed = JSON.parse(potentialJson);
                jsons.push(parsed);
                // 移除已解析部分
                buffer = buffer.slice(endIdx + 1);
                startIdx = buffer.indexOf('{');
                depth = 0;
              } catch (e) {
                console.warn('Invalid JSON found:', potentialJson);
                buffer = buffer.slice(startIdx + 1); // 跳过无法解析的部分
                startIdx = buffer.indexOf('{');
                break;
              }
            } else {
              // 不完整 JSON，跳出循环继续接收数据
              break;
            }
          }

          // 处理提取出的 JSON 数据
          for (const parsed of jsons) {
            if (parsed.event === 'message') {
              aiResponse += parsed.answer;
              this.currentMessages[aiMessageIndex].text = aiResponse;
              //  如果有返回新的 conversation_id，则更新到前端
              //  只有当 conversation_id 存在且非空时才更新
              if (parsed.conversation_id && parsed.conversation_id.trim() !== '') {
                this.conversationId = parsed.conversation_id;
              }
            }
          }

          await this.$nextTick();
          this.scrollToBottom();
        }

        // 更新当前对话标题
        this.updateConversationTitle(q);

        // 如果有错误信息，显示出来
        if (aiResponse.trim() === '') {
          this.currentMessages[aiMessageIndex].text = 'AI 返回了空结果，请稍后再试。';
        }

      } catch (error) {
        console.error('Error:', error);
        this.currentMessages.push({ role: 'ai', text: '请求失败: ' + error.message });
      } finally {
        this.loading = false;
      }
    },

    scrollToBottom() {
      const container = this.$refs.messagesContainer || document.querySelector('.ai-markdown');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },
    createNewChat() {
      const newSessionId = 'session-' + Date.now();
      this.sessionId = newSessionId;

      const newUserId = 'user-' + Date.now(); // 生成唯一 userId

      const newConversation = {
        id: newSessionId,
        title: '新对话',
        messages: [{ role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }],
        createdAt: new Date(),
        modelType: this.currentModel // 记录当前模型类型
      };

      this.conversations.push(newConversation);
      this.switchConversation(newSessionId);
      this.userId = newUserId; // 设置固定 userId

      this.scrollToBottomOfSidebar(); // 新建后尝试滚动
    },

    switchConversation(conversationId) {
      this.currentConversationId = conversationId;
      const conversation = this.conversations.find(c => c.id === conversationId);

      if (conversation) {
        this.currentMessages = [...conversation.messages];
        //this.sessionId = 'session-' + conversationId;
        this.sessionId = conversationId;

        // 如果该对话已存在 userId，则复用；否则生成新的
        this.userId = conversation.userId || 'user-' + Date.now();

        // 存储到当前对话对象中，避免下次切换回来再变
        conversation.userId = this.userId;

        // 将历史对话中的 ai_conversation_id 同步到当前对话状态中
        const lastAIMessage = conversation.messages.find(m => m.role === 'ai');
        this.conversationId = lastAIMessage?.conversationId || '';
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
        conversation.title = firstMessage.length > 20 ? firstMessage.substring(0, 20) + '...' : firstMessage;
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
    const initialSessionId = 'session-' + Date.now();
    const initialUserId = 'user-' + Date.now();

    const initialConversation = {
      id: initialSessionId,
      title: '新对话',
      messages: [{ role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }],
      createdAt: new Date(),
      userId: initialUserId // 初始化 userId
    };

    this.conversations.push(initialConversation);
    this.switchConversation(initialSessionId);
    this.userId = initialUserId;
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
  /*height: 600px;*/
  background: #ffffff;
  padding: 0;
  margin-left: 90px;
}

.chat-content {
  flex: 1;
  width: 1200px;
  margin-left: 0px;
  padding-left: 10px;
  background-color: rgb(255, 255, 255);
  margin-bottom: 200px;
}

.chat-input-bar {
  height: 200px;
  position: fixed;
  width: 800px;
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
  height: 40px;
  line-height: 1.2;
  vertical-align: top;
  border: 0px solid #e6e6e6;
  border-radius: 6px;
  background: #ffffff;
  margin-left: 0px;
  margin-top: 0;
  transition: box-shadow 0.3s ease;
}

.chat-input-bar input:focus {
  box-shadow: 0 0 0px rgba(79, 140, 255, 0.5);
  outline: none;
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

.conversation-item:hover {
  background: #ffffff;
  transform: none;
}

.conversation-item.active {
  border-color: transparent;
  box-shadow: none;
  background-color: powderblue;
}

.conversation-item.active,
.new-chat-btn {
  box-shadow: none;

}

/**用户提问框 */
.user-question {
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
  margin-left: 600px;
}

.send-file,
.ask-model {
  margin-left: 10px;
  height: 50px;
  font-size: 25px;
  width: 50px;
  background-color: #e2dfdf;
  border-radius: 15px;
  border: white;
}

/**提问框 */
.chat-input-bar textarea {
  width: 765px;
  padding: 10px;
  height: 80px;
  line-height: 1.5;
  border: 0px solid #e6e6e6;
  border-radius: 6px;
  background: #ffffff;
  margin-left: 0;
  margin-top: 0;
  resize: none;
  transition: box-shadow 0.3s ease;
}

.chat-input-bar textarea:focus {
  outline: none;
  box-shadow: 0 0 5px rgba(255, 255, 255, 1);
}

.ai-text {
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
  white-space: pre-wrap;
  hyphens: auto;
}

.ai-text h1 {
  font-size: 24px;
  font-weight: bold;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 4px;
}

.ai-text h2 {
  font-size: 20px;
  font-weight: bold;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 4px;
}

.ai-text h3 {
  font-size: 18px;
  font-weight: bold;
  margin-top: 1.2em;
  margin-bottom: 0.4em;
}

.ai-text p {
  margin-top: 0.8em;
  margin-bottom: 0.8em;
}

.ai-text a {
  color: #0366d6;
  text-decoration: none;
}

.ai-text a:hover {
  text-decoration: underline;
}

.ai-text ul, .ai-text ol {
  margin-left: 20px;
  padding-left: 10px;
}

.ai-text li {
  margin-bottom: 0.4em;
}

.ai-text blockquote {
  border-left: 4px solid #dfe2e5;
  padding-left: 1em;
  color: #6a737d;
  margin-left: 0;
  padding-top: 0.5em;
  padding-bottom: 0.5em;
  background-color: #f6f8fa;
  border-radius: 4px;
}

.ai-text code {
  background-color: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 0.9em;
}

.ai-text pre {
  background-color: #f0f0f0;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  margin-top: 1em;
  margin-bottom: 1em;
  box-shadow: inset 0 1px 2px rgba(27,31,35,0.05);
}

.ai-text table {
  display: block;
  width: 100%;
  overflow: auto;
  border-collapse: collapse;
  margin-top: 1em;
  margin-bottom: 1em;
}

.ai-text th, .ai-text td {
  padding: 6px 13px;
  border: 1px solid #dfe2e5;
}

.ai-text th {
  font-weight: bold;
  background-color: #f6f8fa;
}

.ai-text img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 1em 0;
}

.ai-text hr {
  height: 1px;
  background-color: #eaeaea;
  border: none;
  margin: 2em 0;
}

/**选择不同模型的按钮样式*/
.model-selector button {
  width: 100%;
  padding: 12px 16px;
  margin: 8px 0;
  background-color: #ffffff;
  color: #333333;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-icon {
  display: inline-block;
  width: 24px;
  height: 24px;
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.3s ease;
}

.model-selector button:hover {
  background-color: #f5f5f5;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.model-selector button:active {
  transform: translateY(0);
}

.model-selector {
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.conversation-list {
  overflow-y: auto;
  max-height: calc(100vh - 160px);
}

/* 滚动条样式 */
.conversation-list::-webkit-scrollbar {
  width: 6px;
}

.conversation-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.conversation-list::-webkit-scrollbar-track {
  background-color: rgba(0, 0, 0, 0.05);
}


</style>
