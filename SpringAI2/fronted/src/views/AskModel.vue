<template>
  <div class="ask-model">
    <div class="chat-container">
      <div class="messages" ref="messagesContainer">
        <div v-for="(message, index) in messages" :key="index" 
             :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
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
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.message {
  margin-bottom: 20px;
  max-width: 80%;
}

.user-message {
  margin-left: auto;
}

.ai-message {
  margin-right: auto;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
  word-break: break-word;
}

.user-message .message-content {
  background-color: #409EFF;
  color: white;
}

.ai-message .message-content {
  background-color: #f4f4f5;
  color: #333;
}

.input-container {
  padding: 20px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
}

.input-container .el-input {
  flex: 1;
}

.input-container .el-button {
  align-self: flex-end;
}
</style> 