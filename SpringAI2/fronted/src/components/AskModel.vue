<template>
 
  <div class="chat-page">
    <h1>速理通智能问答</h1>
    <div class="chat-content">
      <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-bubble', msg.role]">
        <div class="bubble-content">
          <span v-if="msg.role === 'ai'">🤖</span>
          <!--
          <span v-if="msg.role === 'user'">你</span>
        -->
          <span class="bubble-text">{{ msg.text }}</span>
        </div>
      </div>
      <div v-if="loading" class="chat-bubble ai">
        <div class="bubble-content">
          <span>🤖</span>
          <span class="bubble-text">正在生成回答...</span>
        </div>
      </div>
    </div>
    <div class="chat-input-bar">
      <input v-model="question" placeholder="询问任何问题" @keyup.enter="askModel" />
      <button @click="askModel">发送</button>
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
      messages: [
        { role: 'ai', text: '你好！👋 有什么可以帮你的吗?' }
      ]
    };
  },
  methods: {
    async askModel() {
      if (!this.question) return;
      const userMsg = { role: 'user', text: this.question };
      this.messages.push(userMsg);
      this.loading = true;
      const q = this.question;
      this.question = '';
      try {
        const response = await axios.post('http://localhost:8080/api/llm/ask', {
          message: q,
          sessionId: this.sessionId || '',
          userId: 'user-' + Date.now()
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
        
        this.messages.push({ role: 'ai', text: aiResponse });
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
        this.messages.push({ role: 'ai', text: errorMessage });
      } finally {
        this.loading = false;
      }
    }
  },
  created() {
    this.sessionId = 'session-' + Date.now();
  }
};
</script>

<style scoped>
html, body {
  height: 80%;
  margin: 0;
  padding: 0;
  background: #f4f6fa;
}
.chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
}
.chat-content {
  background: #f4f6fa;
  width: 1200px;
  /*height: 600px;*/
  margin-bottom: 50px;
  /*max-width: 700px;*/
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-sizing: border-box;
  overflow-y: auto;
  padding-bottom: 90px;
}
.chat-bubble {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 80%;
  margin-bottom: 0;
}
.chat-bubble.user {
  align-self: flex-end;
  align-items: flex-end;
}
.chat-bubble.ai {
  align-self: flex-start;
  align-items: flex-start;
}
.bubble-content {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(60,120,200,0.10);
  padding: 16px 22px;
  font-size: 17px;
  color: #222;
  min-width: 60px;
  min-height: 32px;
  gap: 8px;
}
.chat-bubble.user .bubble-content {
  background: linear-gradient(90deg, #e3f0ff 0%, #f8fafc 100%);
  color: #357ae8;
}
.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-input-bar {
  position: fixed;
  /*width: 1200px;*/
  left: 0;
  bottom: 0;
  width: 85vw;
  display: flex;
  margin-left: 125px;
  justify-content: center;
  align-items: center;
  background: rgba(255,255,255,0.95);
  box-shadow: 0 -2px 16px rgba(60,120,200,0.08);
  padding: 18px 0 18px 0;
  z-index: 10;
}
.chat-input-bar input {
  width: 1200px;
  max-width: 95vw;
  padding: 16px 20px;
  font-size: 17px;
  border: 1.5px solid #e0e3e8;
  border-radius: 24px;
  outline: none;
  background: #f8fafc;
  margin-right: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(60,120,200,0.06);
}
.chat-input-bar input:focus {
  border-color: #4f8cff;
  box-shadow: 0 0 0 2px #e3f0ff;
}
.chat-input-bar button {
  padding: 0 28px;
  height: 48px;
  border-radius: 24px;
  background: linear-gradient(90deg, #4f8cff 0%, #6fc3ff 100%);
  color: #fff;
  border: none;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(79,140,255,0.08);
  transition: background 0.2s, transform 0.1s;
}
.chat-input-bar button:hover, .chat-input-bar button:focus {
  background: linear-gradient(90deg, #357ae8 0%, #4f8cff 100%);
  transform: translateY(-2px) scale(1.04);
}
@media (max-width: 700px) {
  .chat-content {
    max-width: 100vw;
    padding: 16px 0 100px 0;
  }
  .chat-input-bar input {
    width: 98vw;
    max-width: 98vw;
    font-size: 16px;
    padding: 12px 10px;
  }
  .chat-input-bar button {
    padding: 0 14px;
    height: 40px;
    font-size: 16px;
  }
}
</style>
