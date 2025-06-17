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
        <div 
          v-for="(conversation, index) in conversations" 
          :key="conversation.id"
          :class="['conversation-item', { active: currentConversationId === conversation.id }]"
          @click="switchConversation(conversation.id)"
        >
          <div class="conversation-info">
            <div class="conversation-title">{{ conversation.title || '新对话' }}</div>
            <div class="conversation-time">{{ formatTime(conversation.createdAt) }}</div>
          </div>
          <button 
            class="delete-btn" 
            @click.stop="deleteConversation(conversation.id)"
            v-if="conversations.length > 1"
          >
            🗑️
          </button>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-page">
      <div class="chat-content">
        <div v-for="(msg, idx) in currentMessages" :key="idx" :class="['chat', msg.role]">
          <div v-if="msg.role === 'ai'" class="ai-answer">
            <span ></span>
            <span class="ai-text">{{ msg.text }}</span>
          </div>
          <div v-if="msg.role === 'user'" class="user-question">
            <span ></span>
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
        <input v-model="question" placeholder="询问任何问题" @keyup.enter="askModel" />
        <button class="send-file" @click="sendFile">📂</button> 
        <button class="ask-model" @click="askModel">⬆</button> 
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
/* 优化消息容器布局 */
.messages {
  padding: 20px 12% !important;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 增强消息卡片视觉效果 */
.message {
  max-width: 88%;
  margin-bottom: 0;
  transition: all 0.2s ease;
}

.message:hover {
  transform: scale(1.008);
}

@keyframes dot-animation {
  0%, 100% { opacity: 0.3 }
  50% { opacity: 1 }
}

/* 优化字体排版 */
.message-content {
  font-size: 15px;
  line-height: 1.7;
  padding: 16px 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell;
}

/* 调整头像尺寸和间距 */
.message-avatar {
  width: 36px !important;
  height: 36px !important;
  font-size: 13px !important;
  margin: 0 14px !important;
}

/* 优化输入容器设计 */
.input-container {
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  box-shadow: 0 -4px 20px rgba(79, 140, 255, 0.06);
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
  background: #f7f7f8;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  background: #f7f7f8;
  border-bottom: 1px solid #e6e6e6;
}

.sidebar-header h2 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.new-chat-btn {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  color: #333;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
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
  box-shadow: none;
}

.chat-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 0;
  margin: 0;
}

.chat-content {
  flex: 1;
  width:1200px;
  margin-left: 80px;
  padding-left: 10px;
  background-color: rgb(255, 255, 255);
  margin-bottom: 200px;
}

.chat-input-bar {
  position: fixed;
  left: 260px;
  bottom: 0;
  right: 0;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e6e6e6;
}

.chat-input-bar input {
  width: 800px;
  padding: 20px 20px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  background: #fff;
  margin-left: 200px;
}

/**提问按钮 */
.chat-input-bar button.ask-model {
  padding: 0 15px;
  height: 50px;
  margin-left: 10px;
  border-radius: 20px;
  background: #4f8cff;
  font-size: 24px;  /* 增大字体 */
  /*font-weight: bold;  /* 加粗 */
  color: white;  /* 白色文字 */
  line-height: 50px; /* 垂直居中 */
}

/**上传文件按钮 */
.chat-input-bar button.send-file {
  padding: 0 10px;
  height: 50px;
  margin-left: 10px;
  border-radius: 20px;
  background: #4f8cff;
  font-size: 24px;  /* 增大字体 */
  /*font-weight: bold;  /* 加粗 */
  color: white;  /* 白色文字 */
  line-height: 50px; /* 垂直居中 */
}

.conversation-item {
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 6px;
}

.conversation-item:hover {
  background: #ececf1;
  transform: none;
}

.conversation-item.active {
  background: #ececf1;
  border-color: transparent;
  box-shadow: none;
}

.conversation-item.active,
.new-chat-btn
 {
  background: #fff;
  box-shadow: none;
}

.answer-content{
  background-color: rgb(221, 205, 205);
}

/**用户提问框 */
.user-question{
   /**采用相对定位，固定在右侧 */
   position: relative;
  left: 50%;
  /*width: 600px;*/
  min-width: none;
  max-width: 600px;
  background-color: rgb(245, 245, 245);
  border-radius: 18px 18px 0 18px; /* 改用px单位更精确控制 */
  padding: 15px 20px; /* 增加左右内边距 */
  box-shadow: 0 2px 8px rgba(0,0,0,0.08); /* 添加轻微阴影增强立体感 */
  margin-bottom: 16px; /* 增加底部间距 */
  transition: all 0.3s ease; /* 添加过渡动画 */
}

/**大模型回答框 */
.ai-answer{
  /**采用相对定位，固定在左侧 */
  position: relative;
  /*right: 2%;*/
  width: 1200px;
  background-color: rgb(245, 245, 245);
  border-radius: 18px 18px 18px 0; /* 左上、右上、右下圆角 */
  padding: 15px 20px; /* 内边距 */
  box-shadow: 0 2px 8px rgba(0,0,0,0.05); /* 轻微阴影 */
  margin-bottom: 16px; /* 底部间距 */
  transition: all 0.3s ease; /* 过渡动画 */
}



</style>
