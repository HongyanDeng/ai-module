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
          <button class="ask-model" @click="askModel">⬆</button>
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
    // async askModel() {
    //   if (!this.question) return;
    //
    //   const userMsg = { role: 'user', text: this.question };
    //   this.currentMessages.push(userMsg);
    //   this.loading = true;
    //   const q = this.question;
    //   this.question = '';
    //
    //   try {
    //     const response = await axios.post('http://localhost:8080/api/llm/ask', {
    //       message: q,
    //       sessionId: this.sessionId || '',
    //       //sessionId: this.sessionId,
    //       userId: 'user-' + Date.now()
    //       //userId:this.userId,
    //     });
    //
    //     let aiResponse = '';
    //     if (response.data && response.data.answer) {
    //       aiResponse = response.data.answer;
    //       // 处理换行符
    //       aiResponse = aiResponse.replace(/\\n/g, '\n');
    //       // 移除末尾的 "//"
    //       if (aiResponse.endsWith("//")) {
    //         aiResponse = aiResponse.substring(0, aiResponse.length() - 2);
    //       }
    //     } else if (response.data && response.data.error) {
    //       aiResponse = '错误: ' + response.data.error;
    //     } else {
    //       aiResponse = '抱歉，我无法理解这个回答。';
    //     }
    //
    //     this.currentMessages.push({ role: 'ai', text: aiResponse });
    //
    //     // 更新当前对话的标题（使用第一条用户消息）
    //     this.updateConversationTitle(q);
    //     await this.$nextTick();
    //     this.scrollToBottom();
    //   } catch (error) {
    //     console.error('Error:', error);
    //     let errorMessage = '请求失败';
    //     if (error.response) {
    //       if (error.response.data && error.response.data.error) {
    //         errorMessage = error.response.data.error;
    //       } else if (error.response.data && error.response.data.message) {
    //         errorMessage = error.response.data.message;
    //       } else {
    //         errorMessage = error.response.data || error.response.statusText;
    //       }
    //     } else if (error.message) {
    //       errorMessage = error.message;
    //     }
    //     this.currentMessages.push({ role: 'ai', text: errorMessage });
    //   } finally {
    //     this.loading = false;
    //   }
    // },
    async askModel() {
      if (!this.question) return;

      const userMsg = { role: 'user', text: this.question };
      this.currentMessages.push(userMsg);
      this.loading = true;
      const q = this.question;
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
            userId: 'user-' + Date.now()
          })
        });

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
  /* 固定高度 */
  line-height: 1.5;
  border: 0px solid #e6e6e6;
  border-radius: 6px;
  background: #ffffff;
  margin-left: 0;
  margin-top: 0;
  resize: none;
  /* 禁止手动调整大小 */
  transition: box-shadow 0.3s ease;
}

.chat-input-bar textarea:focus {
  outline: none;
  box-shadow: 0 0 5px rgba(255, 255, 255, 1);
}
</style>
