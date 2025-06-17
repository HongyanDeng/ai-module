<template>
  <div class="ask-model">
    <div class="chat-container">
      <div class="messages" ref="messagesContainer">
        <div v-for="(message, index) in messages" :key="index" 
             :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
          <!-- 新增头像部分 -->
          <el-avatar 
            class="message-avatar"
            :class="{ 'ai-avatar': message.role !== 'user' }"
            :style="message.role === 'user' ? { order: 2 } : {}">
            {{ message.role === 'user' ? '我' : 'AI' }}
          </el-avatar>
          <div class="message-content">{{ message.content }}</div>
        </div>
      </div>
      <div class="input-container">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          @keyup.enter.ctrl="sendMessage"
        />
        <el-button type="primary" @click="sendMessage" :loading="loading">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const sessionId = ref('')

// 生成会话ID
const generateSessionId = () => {
  return 'session-' + Date.now()
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  if (loading.value) return

  try {
    loading.value = true
    
    // 如果是新会话，生成新的会话ID
    if (!sessionId.value) {
      sessionId.value = generateSessionId()
    }

    // 添加用户消息到列表
    messages.value.push({
      role: 'user',
      content: userInput.value
    })

    // 发送请求到后端
    const response = await axios.post('http://localhost:8080/api/llm/ask', {
      message: userInput.value,
      sessionId: sessionId.value,
      userId: 'user-' + Date.now()
    })

    // 添加AI响应到列表
    if (response.data.answer) {
      messages.value.push({
        role: 'assistant',
        content: response.data.answer
      })
    }

    // 清空输入框
    userInput.value = ''
    
    // 滚动到底部
    await scrollToBottom()
  } catch (error) {
    console.error('Error:', error)
    ElMessage.error(error.response?.data?.error || '发送消息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ask-model {
  padding: 20px 20px 20px 20px; /* 减少左侧padding */
  align-items: flex-start; /* 从center改为flex-start */
}

.chat-container {
  max-width: 80%; /* 移除1200px限制 */
  margin-left: 8px; /* 新增左侧偏移 */
}

.message {
  max-width: 84%; /* 扩大消息宽度 */
}

.user-message {
  margin-right: 10px; /* 减少右侧边距 */
}

/* 在原有代码基础上修改以下内容 */
.ai-message {
  margin-left: 20px; /* 增加左侧边距 */
  margin-right: auto;
}

.messages {
  padding: 20px 15px; /* 调整左右padding */
}

/* 新增头像样式 */
.message-avatar {
  flex-shrink: 0;
  margin: 0 12px;
  background-color: var(--el-color-primary);
  color: white;
}

.message {
  display: flex;
  align-items: start;
  margin-bottom: 24px;
  max-width: 76%;
  transition: all 0.3s ease;
}

.chat-content{
  
}

/* 优化消息气泡样式 */
.message-content {
  padding: 14px 18px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.user-message .message-content {
  background: var(--el-color-primary);
  border-radius: 12px 12px 0 12px;
}

.ai-message .message-content {
  background: linear-gradient(145deg, #f8f9ff, #e3f2fd);
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 18px 18px 18px 4px;
  color: #2c3e50;
  position: relative;
  transition: transform 0.2s ease;
}

/* 添加对话气泡小箭头 */
.ai-message .message-content::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 12px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-right-color: var(--el-color-primary-light-5);
}

/* 优化头像样式 */
.message-avatar.ai-avatar {
  background-color: var(--el-color-success);
  margin-right: 8px;
}

/* 添加悬停效果 */
.message:hover .message-content {
  transform: translateX(3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 优化滚动条样式 */
.messages::-webkit-scrollbar {
  width: 6px;
}

.messages::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

/* 新增消息加载动画 */
@keyframes message-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message {
  animation: message-appear 0.3s ease;
}

/* 调整输入容器间距 */
.input-container {
  padding: 16px 20px;
  gap: 12px;
}
.input-container .el-input {
  flex: 1;
}

.input-container .el-button {
  align-self: flex-end;
}
</style>