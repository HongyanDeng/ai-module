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
        const response = await axios.post('http://localhost:8080/api/llm/ask', {
          inputs: {},
          query: this.question,
          response_mode: 'blocking',
          conversation_id: '',
          user: 'abc-123',
          answer: ''
        });
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

<style scoped>
.ask-model {
  max-width: 600px;
  margin: 40px auto;
}
input {
  width: 100%;
  padding: 10px;
  font-size: 16px;
}
button {
  margin-top: 10px;
  padding: 10px 20px;
}
pre {
  white-space: pre-wrap;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}
</style>
