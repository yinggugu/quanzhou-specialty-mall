<template>
  <div class="ai-chat-wrap">
    <!-- 聊天主体 -->
    <div class="ai-chat-panel">
      <div class="ai-chat-header">
        <h2 class="ai-chat-title">AI 小助手</h2>
        <span class="ai-service-tip">在线客服 · 9:00-18:00</span>
      </div>

      <div class="ai-chat-scroll" ref="chatDom">
        <div v-for="(msg, idx) in chatMsgArr" :key="idx" class="ai-bubble-row" :class="msg.isUser ? 'user-side' : 'service-side'">
          <div class="ai-bubble-box">
            <div class="ai-bubble-txt">{{ msg.content }}</div>
            <div class="ai-bubble-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>
      </div>

      <div class="ai-chat-input-bar">
        <div class="ai-input-wrapper">
          <input v-model="inputChatVal" class="ai-chat-input" placeholder="请输入您的问题…"
                 @keydown.enter="submitMsg" />
          <button class="ai-send-btn" @click="submitMsg">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiChat',
  data() {
    return {
      inputChatVal: '',
      ws: null, wsConnected: false, sessionId: null, userId: null, username: '',
      chatMsgArr: []
    }
  },
  mounted() {
    this.initUser(); this.connectWebSocket()
  },
  beforeDestroy() {
    if (this.ws) { this.ws.close(); this.ws = null }
  },
  methods: {
    initUser() {
      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (user) { this.userId = user.id; this.username = user.username }
    },
    formatTime(raw) {
      if (!raw) return ''
      const d = new Date(raw), pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    scrollBottom() { const d = this.$refs.chatDom; if (d) d.scrollTop = d.scrollHeight },

    connectWebSocket() {
      const token = localStorage.getItem('token')
      if (!token || !this.userId) {
        this.chatMsgArr = [{ isUser: false, content: '您好！我是AI小助手，请问有什么可以帮您？', time: new Date().toISOString() }]
        return
      }
      const host = process.env.VUE_APP_WS_URL || 'localhost:8088'
      const url = `${location.protocol==='https:'?'wss:':'ws:'}//${host}/ws/ai-chat?token=${encodeURIComponent(token)}`
      // 防止重复连接造成重连风暴
      if (this.ws && (this.ws.readyState === WebSocket.CONNECTING || this.ws.readyState === WebSocket.OPEN)) {
        return
      }
      this.ws = new WebSocket(url)
      this.ws.onopen = () => {
        this.wsConnected = true
        this._reconnectAttempts = 0
      }
      this.ws.onmessage = (event) => {
        try { const data = JSON.parse(event.data); this.handleWsMessage(data) } catch (e) {}
      }
      this.ws.onclose = () => {
        this.wsConnected = false
        // 最多重连5次，避免无限风暴
        this._reconnectAttempts = (this._reconnectAttempts || 0) + 1
        if (this._reconnectAttempts <= 5) {
          setTimeout(() => { if (!this.wsConnected) this.connectWebSocket() }, 3000)
        } else {
          console.warn('AI助手 WebSocket 重连失败，已达最大重试次数。请确认后端是否已重启。')
        }
      }
      this.ws.onerror = () => {
        this.wsConnected = false
        // 首次连接失败时给出明确提示
        if (!this._reconnectAttempts) {
          this.chatMsgArr = [{ isUser: false, content: 'AI助手连接中…如持续失败请确认后端服务已重启。', time: new Date().toISOString() }]
        }
      }
    },

    handleWsMessage(data) {
      switch (data.type) {
        case 'sessionCreated':
          this.sessionId = data.sessionId
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type: 'loadHistory', sessionId: this.sessionId }))
          }
          break
        case 'historyMessages':
          if (data.messages && data.messages.length > 0) {
            this.chatMsgArr = data.messages.map(m => ({ isUser: !m.isAdmin, content: (m.content||'').trim(), time: m.time || new Date().toISOString() }))
          } else {
            this.chatMsgArr = [{ isUser: false, content: '您好！我是AI小助手，请问有什么可以帮您？', time: new Date().toISOString() }]
          }
          this.$nextTick(() => this.scrollBottom())
          break
        case 'newMessage':
          if (data.isAdmin) {
            this.chatMsgArr.push({ isUser: false, content: (data.content||'').trim(), time: data.time || new Date().toISOString() })
            this.$nextTick(() => this.scrollBottom())
          }
          break
      }
    },

    submitMsg(e) {
      if (e) e.preventDefault()
      const content = this.inputChatVal.trim(); if (!content) return
      const now = new Date().toISOString()
      this.chatMsgArr.push({ isUser: true, content, time: now })
      this.inputChatVal = ''
      this.$nextTick(() => this.scrollBottom())
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'message', sessionId: this.sessionId || 0, msgType: 'text', content }))
      }
    }
  }
}
</script>

<style scoped>
.ai-chat-wrap { width: 100%; height: calc(100vh - 100px); display: flex; justify-content: center; padding: 16px 20px; box-sizing: border-box; }
.ai-chat-panel { width: 720px; max-width: 96vw; height: 100%; background: #fff; border-radius: 18px; box-shadow: 0 2px 8px rgba(92,51,23,0.04), 0 8px 32px rgba(92,51,23,0.06); display: flex; flex-direction: column; overflow: hidden; }
.ai-chat-header { display: flex; justify-content: space-between; align-items: baseline; padding: 20px 28px 12px; border-bottom: 2px solid #ede6dc; flex-shrink: 0; }
.ai-chat-title { margin: 0; font-size: 18px; font-weight: 600; color: #3d2b1a; }
.ai-service-tip { font-size: 12px; color: #b0957a; }
.ai-chat-scroll { flex: 1; overflow-y: auto; padding: 16px 28px; min-height: 220px; }
.ai-bubble-row { margin-bottom: 16px; display: flex; }
.service-side { justify-content: flex-start; }
.user-side { justify-content: flex-end; }
.ai-bubble-box { max-width: 70%; display: flex; flex-direction: column; }
.user-side .ai-bubble-box { align-items: flex-end; }
.service-side .ai-bubble-box { align-items: flex-start; }
.ai-bubble-txt { padding: 8px 14px; border-radius: 14px; font-size: 14px; line-height: 1.5; word-break: break-word; }
.service-side .ai-bubble-txt { background: #f7f4f1; color: #3d2b1a; border-bottom-left-radius: 6px; }
.user-side .ai-bubble-txt { background: #5c3317; color: #fff; border-bottom-right-radius: 6px; }
.ai-bubble-time { font-size: 11px; color: #c4b49e; margin-top: 4px; }
.ai-chat-input-bar { padding: 12px 28px 20px; flex-shrink: 0; }
.ai-input-wrapper { display: flex; gap: 8px; align-items: center; background: #faf7f3; border-radius: 22px; padding: 5px 5px 5px 16px; border: 1.5px solid #ede6dc; transition: border-color .2s; }
.ai-input-wrapper:focus-within { border-color: #c9a87c; background: #fff; }
.ai-chat-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: #3d2b1a; line-height: 36px; font-family: inherit; }
.ai-chat-input::placeholder { color: #c4b49e; }
.ai-send-btn { background: #5c3317; border: none; color: #fff; border-radius: 18px; padding: 8px 20px; font-size: 14px; cursor: pointer; flex-shrink: 0; font-family: inherit; }
.ai-send-btn:hover { background: #3d2b1a; }
.ai-chat-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.ai-chat-scroll::-webkit-scrollbar { display: none; }
</style>
