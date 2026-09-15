<template>
  <div class="service-container">
    <div class="work-bench">
      <!-- 左侧会话列表 -->
      <div class="session-panel">
        <div class="panel-head">
          <h3 class="panel-title">在线咨询</h3>
          <span class="session-count">{{ sessionList.length }} 个会话</span>
        </div>
        <div
          v-for="session in sessionList"
          :key="session.id"
          class="session-item"
          :class="{active: activeSessionId === session.id}"
          @click="switchSession(session.id)"
          @contextmenu.prevent="showContextMenu($event, session)"
        >
          <div class="session-avatar">{{ session.username.slice(-2) }}</div>
          <div class="session-text">
            <div class="session-name-row">
              <span class="session-name">{{ session.username }}</span>
              <span v-if="session.unread > 0" class="unread-badge">{{ session.unread }}</span>
            </div>
            <div class="session-last-tip">{{ session.desc }}</div>
          </div>
        </div>
      </div>


        <!-- ✅ 右键菜单弹窗 -->
        <div
          v-if="contextMenu.visible"
          class="context-menu"
          :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
          @mouseleave="contextMenu.visible = false"
        >
          <div class="context-menu-item" @click="markUnreadSession">标为未读</div>
          <div class="context-menu-item context-menu-danger" @click="deleteSessionMsgs">删除聊天记录</div>
        </div>

      <!-- 右侧聊天主体 -->
      <div class="chat-main">
        <div class="chat-top-bar">
          <div class="chat-title-row">
            <span class="chat-title">{{ currentSession.username }}</span>
            <span class="chat-subtitle">咨询中</span>
          </div>
          <span class="status-tag" :class="statusClass">{{ currentSession.status }}</span>
        </div>

        <!-- 聊天气泡区域 -->
        <div class="message-wrap" ref="msgBox">
          <div
            v-for="(msg, index) in currentSession.msgArr"
            :key="index"
            class="msg-row"
            :class="msg.isAdmin ? 'admin-side' : 'user-side'"
          >
            <div class="bubble-wrap">
              <template v-if="msg.msgType === 'orderCard' && msg.orderInfo">
                <div class="bubble order-bubble-card">
                  <div class="obc-img" v-if="getCardImage(msg.orderInfo.goodsImg)">
                    <img v-if="getCardImage(msg.orderInfo.goodsImg)" :src="getCardImage(msg.orderInfo.goodsImg)" class="obc-goods-img" alt="">
                  </div>
                  <div class="obc-info">
                    <p class="obc-title">{{ msg.orderInfo.goodsTitle }}</p>
                    <p class="obc-price">¥{{ msg.orderInfo.totalPrice }}</p>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="bubble">{{ msg.content }}</div>
              </template>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>
        </div>


        <!-- 底部输入、按钮 -->
        <div class="input-footer">
          <div class="input-wrapper">
            <input
              v-model="inputMsg"
              placeholder="请输入回复内容…"
              class="chat-input"
              :disabled="isFinish"
              @keydown.enter="sendMsg"
            >
            <button class="send-btn" @click="sendMsg" :disabled="isFinish">发送</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ServiceChatPage",
  data() {
    return {
      activeSessionId: null,
      inputMsg: "",
      // WebSocket
      ws: null,
      wsConnected: false,
      // 会话列表（从服务端 WebSocket 推送加载）
      sessionList: [],
      // ✅ 右键菜单状态
      contextMenu: { visible: false, x: 0, y: 0, session: null }
    };
  },
  computed: {
    currentSession() {
      const s = this.sessionList.find(item => item.id === this.activeSessionId);
      return s || { username: '', status: '', orderNo: '', goodsName: '', consultType: '其他问题', remark: '', msgArr: [] };
    },
    isFinish() {
      return this.currentSession.status === "已完成";
    },
    statusClass() {
      const map = { '待回复': 's-pending', '进行中': 's-active', '已完成': 's-done' }
      return map[this.currentSession.status] || ''
    }
  },
  mounted() {
    this.connectWebSocket()
  },
  beforeDestroy() {
    if (this.ws) { this.ws.close(); this.ws = null; }
  },
  methods: {
    /* ============ WebSocket 连接 ============ */
    connectWebSocket() {
      const token = localStorage.getItem('token')
      if (!token) {
        return
      }
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = process.env.VUE_APP_WS_URL || 'localhost:8088'
      const url = `${protocol}//${host}/ws/chat?token=${encodeURIComponent(token)}`
      this.ws = new WebSocket(url)
      this.ws.onopen = () => {
        this.wsConnected = true
        // 请求会话列表
        this.ws.send(JSON.stringify({ type: 'loadSessions' }))
      }
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.handleWsMessage(data)
        } catch (e) {
          console.error('WS消息解析失败:', e)
        }
      }
      this.ws.onclose = () => {
        this.wsConnected = false
        setTimeout(() => { if (!this.wsConnected) this.connectWebSocket() }, 3000)
      }
      this.ws.onerror = () => { this.wsConnected = false }
    },

    /* ============ 处理 WebSocket 消息 ============ */
    handleWsMessage(data) {
      switch (data.type) {
        case 'sessionList':
          this.mergeSessionList(data.sessions || [])
          break

        case 'newMessage':
          this.handleNewMessage(data)
          break

        case 'historyMessages':
          this.handleHistoryMessages(data)
          break
      }
    },

    /* ============ 合并服务端会话列表（保留本地 msgArr） ============ */
    mergeSessionList(serverSessions) {
      const merged = serverSessions.map(ss => {
        const existing = this.sessionList.find(l => l.id === ss.id)
        // 当前活跃会话保持未读为0
        let unread = ss.unread || 0
        if (ss.id === this.activeSessionId) {
          unread = 0
        }
        return {
          id: ss.id,
          username: ss.username,
          desc: ss.desc || '',
          unread: unread,
          orderNo: ss.orderNo || '',
          goodsName: ss.goodsName || '',
          consultType: ss.consultType || '其他问题',
          status: ss.status || '进行中',
          remark: ss.remark || '',
          msgArr: existing ? existing.msgArr : []
        }
      })
      this.sessionList = merged
      // 自动选中第一个
      if (!this.activeSessionId && this.sessionList.length > 0) {
        this.activeSessionId = this.sessionList[0].id
        this.loadHistory(this.activeSessionId)
      }
    },

    /* ============ 新消息到达 ============ */
    handleNewMessage(data) {
      const sid = data.sessionId
      let session = this.sessionList.find(s => s.id === sid)
      if (!session) {
        session = {
          id: sid,
          username: data.username || '未知用户',
          desc: data.msgType === 'orderCard' ? '[订单卡片]' : (data.content || '').substring(0, 30),
          unread: 0,
          orderNo: '',
          goodsName: '',
          consultType: '其他问题',
          status: '进行中',
          remark: '',
          msgArr: []
        }
        this.sessionList.unshift(session)
      }
      // 去重：相同 msgId 不重复添加
      if (data.msgId && session.msgArr.some(m => m.msgId === data.msgId)) return
      // 追加消息（isAdmin 强制布尔）
      const isAdminMsg = data.isAdmin === true || data.isAdmin === 'true' || data.isAdmin === 1
      session.msgArr.push({
        msgId: data.msgId,
        isAdmin: isAdminMsg,
        msgType: data.msgType || 'text',
        content: (data.content || '').trim(),
        time: data.time || '',
        orderInfo: data.orderInfo || null
      })
      // 非当前会话增加未读数
      if (sid !== this.activeSessionId) {
        session.unread = (session.unread || 0) + 1
      }
      // 更新描述
      session.desc = data.msgType === 'orderCard' ? '[订单卡片]' : (data.content || '').substring(0, 30)
      this.$nextTick(() => { if (sid === this.activeSessionId) this.scrollBottom() })
    },

    /* ============ 历史消息 ============ */
    handleHistoryMessages(data) {
      const session = this.sessionList.find(s => s.id === data.sessionId)
      if (session) {
        session.msgArr = (data.messages || []).map(m => ({
          isAdmin: m.isAdmin === true || m.isAdmin === 'true' || m.isAdmin === 1,
          msgType: m.msgType || 'text',
          content: (m.content || '').trim(),
          time: m.time || '',
          orderInfo: m.orderInfo || null
        }))
        this.$nextTick(() => { this.scrollBottom() })
      }
    },

    /* ============ 加载历史消息 ============ */
    loadHistory(sessionId) {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'loadHistory', sessionId }))
      }
    },

    /* ============ 切换会话 ============ */
    switchSession(id) {
      // 离开当前会话前同步信息
      if (this.activeSessionId && this.activeSessionId !== id) {
        this.syncCurrentSessionInfo()
      }
      this.activeSessionId = id
      const target = this.sessionList.find(i => i.id === id)
      if (target) {
        target.unread = 0
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify({ type: 'markRead', sessionId: id }))
        }
        if (!target.msgArr || target.msgArr.length === 0) {
          this.loadHistory(id)
        }
      }
      this.$nextTick(() => this.scrollBottom())
    },

    /* ============ 同步当前会话信息到服务端 ============ */
    syncCurrentSessionInfo() {
      if (!this.activeSessionId || !this.ws || this.ws.readyState !== WebSocket.OPEN) return
      const s = this.sessionList.find(i => i.id === this.activeSessionId)
      if (!s) return
      this.ws.send(JSON.stringify({
        type: 'updateSession',
        sessionId: this.activeSessionId,
        consultType: s.consultType,
        remark: s.remark,
        status: s.status
      }))
    },

    /* ============ 发送消息 ============ */
    sendMsg(e) {
      if (e) e.preventDefault()
      const val = this.inputMsg.trim()
      if (!val) {
        this.$message.warning("回复内容不能为空")
        return
      }
      if (!this.activeSessionId) return

      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const timeStr = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`

      // 本地展示
      this.currentSession.msgArr.push({ isAdmin: true, content: val, time: timeStr })
      this.inputMsg = ""
      this.$nextTick(() => this.scrollBottom())

      // WebSocket 发送
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'message',
          sessionId: this.activeSessionId,
          msgType: 'text',
          content: val
        }))
      }
    },

    /* ============ 标记已完成 ============ */
    markFinished() {
      const session = this.currentSession
      if (!session || !this.activeSessionId) return
      session.status = "已完成"
      this.$message.success("本次咨询已标记完成")
      // 同步到服务端
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'updateSession',
          sessionId: this.activeSessionId,
          status: '已完成'
        }))
      }
    },

    /* ============ 订单卡片图片 ============ */
    getCardImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    /* ============ 滚动到底 ============ */
    scrollBottom() {
      const dom = this.$refs.msgBox
      if (dom) dom.scrollTop = dom.scrollHeight
    },

    /* ============ ✅ 右键菜单 ============ */
    showContextMenu(event, session) {
      this.contextMenu.visible = true
      this.contextMenu.x = event.clientX
      this.contextMenu.y = event.clientY
      this.contextMenu.session = session
      // 点击页面任意位置关闭菜单
      const closeMenu = () => {
        this.contextMenu.visible = false
        document.removeEventListener('click', closeMenu)
      }
      setTimeout(() => document.addEventListener('click', closeMenu), 0)
    },

    async markUnreadSession() {
      this.contextMenu.visible = false
      const session = this.contextMenu.session
      if (!session) return
      try {
        const token = localStorage.getItem('token')
        const res = await fetch('/api/admin/chat/markUnread', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify({ sessionId: session.id })
        })
        const data = await res.json()
        if (data.code === 200) {
          session.unread = 1
          session.status = '待回复'
          this.$message.success('已标记为未读')
        }
      } catch (e) {
        console.error('标记未读失败:', e)
      }
    },

    async deleteSessionMsgs() {
      this.contextMenu.visible = false
      const session = this.contextMenu.session
      if (!session) return
      try {
        const token = localStorage.getItem('token')
        const res = await fetch('/api/admin/chat/delSessionMsg', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify({ sessionId: session.id })
        })
        const data = await res.json()
        if (data.code === 200) {
          // ✅ 从列表移除该会话
          const idx = this.sessionList.findIndex(s => s.id === session.id)
          if (idx !== -1) this.sessionList.splice(idx, 1)
          // 如果删除的是当前活跃会话，切换到第一个
          if (this.activeSessionId === session.id) {
            this.activeSessionId = this.sessionList.length > 0 ? this.sessionList[0].id : null
            if (this.activeSessionId) this.loadHistory(this.activeSessionId)
          }
          this.$message.success('聊天记录已删除')
        }
      } catch (e) {
        console.error('删除聊天记录失败:', e)
      }
    }
  }
};
</script>

<style scoped>
/* ========================================
   全局
   ======================================== */
.service-container {
  width: 100%;
  height: calc(100vh - 110px);
  background: #f7f3ed;
  overflow: hidden;
}
.work-bench {
  display: flex;
  padding: 16px;
  gap: 16px;
  height: 100%;
  box-sizing: border-box;
}

/* ========================================
   左侧会话列表
   ======================================== */
.session-panel {
  width: 250px;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 6px rgba(92, 51, 23, 0.04);
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #ede6dc;
}
.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #3d2b1a;
}
.session-count {
  font-size: 11px;
  color: #b0957a;
}
.session-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all 0.2s ease;
}
.session-item:hover {
  background: #fdf9f3;
}
.session-item.active {
  background: #fdf6ee;
  box-shadow: 0 2px 8px rgba(92, 51, 23, 0.06);
}
.session-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f5f0e8;
  color: #8b6914;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.session-text {
  flex: 1;
  overflow: hidden;
}
.session-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.session-name {
  font-size: 14px;
  color: #3d2b1a;
  font-weight: 500;
}
.unread-badge {
  background: #d43c2e;
  color: #fff;
  border-radius: 8px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 500;
}
.session-last-tip {
  font-size: 12px;
  color: #b0957a;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========================================
   右侧聊天
   ======================================== */
.chat-main {
  flex: 1;
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  box-shadow: 0 1px 6px rgba(92, 51, 23, 0.04);
}

/* ---- 顶部栏 ---- */
.chat-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 2px solid #ede6dc;
  flex-shrink: 0;
}
.chat-title-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: #3d2b1a;
}
.chat-subtitle {
  font-size: 12px;
  color: #b0957a;
}
.status-tag {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 10px;
}
.s-pending { color: #d48806; background: #fffbe6; }
.s-active { color: #1677ff; background: #e6f4ff; }
.s-done { color: #7c7c7c; background: #f5f5f5; }

/* ---- 气泡区 ---- */
.message-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 12px 4px;
  min-height: 0;
}
.msg-row {
  margin-bottom: 16px;
  display: flex;
}
.user-side {
  justify-content: flex-start;
}
.admin-side {
  justify-content: flex-end;
}
.bubble-wrap {
  display: flex;
  flex-direction: column;
  max-width: 62%;
}
.user-side .bubble-wrap {
  align-items: flex-start;
}
.admin-side .bubble-wrap {
  align-items: flex-end;
}
.bubble {
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.6;
  font-size: 13.5px;
  word-break: break-word;
}
.user-side .bubble {
  background: #f7f4f1;
  color: #3d2b1a;
  border-bottom-left-radius: 6px;
}
.admin-side .bubble {
  background: #5c3317;
  color: #fff;
  border-bottom-right-radius: 6px;
}
.msg-time {
  font-size: 11px;
  color: #c4b49e;
  margin-top: 4px;
}
.user-side .msg-time { text-align: left; }
.admin-side .msg-time { text-align: right; }

/* ---- 底部输入 ---- */
.input-footer {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
}
.input-wrapper {
  flex: 1;
  display: flex;
  gap: 8px;
  align-items: center;
  background: #faf7f3;
  border-radius: 20px;
  padding: 4px 4px 4px 14px;
  border: 1.5px solid #ede6dc;
  transition: border-color 0.2s;
}
.input-wrapper:focus-within {
  border-color: #c9a87c;
  background: #fff;
}
.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13.5px;
  color: #3d2b1a;
  line-height: 32px;
  font-family: inherit;
}
.chat-input::placeholder {
  color: #c4b49e;
}
.chat-input:disabled {
  color: #b0957a;
}
.send-btn {
  background: #5c3317;
  border: none;
  color: #fff;
  border-radius: 16px;
  padding: 7px 18px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
  font-family: inherit;
}
.send-btn:hover { background: #3d2b1a; }
.send-btn:disabled { background: #c8a88c; cursor: not-allowed; }

/* ---- 滚动条 ---- */
.message-wrap {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.message-wrap::-webkit-scrollbar {
  display: none;
}

/* ---- 订单卡片气泡 ---- */
.order-bubble-card {
  display: flex !important;
  gap: 10px;
  background: #fff !important;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #ede6dc;
  width: 260px;
  box-sizing: border-box;
  color: #3d2b1a !important;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.obc-img {
  width: 74px;
  height: 74px;
  border-radius: 8px;
  flex-shrink: 0;
  overflow: hidden;
  background: #faf6f0;
}
.obc-goods-img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
}
.obc-info {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.obc-title {
  font-size: 13px;
  color: #3d2b1a;
  margin: 0 0 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.obc-price {
  font-size: 16px;
  color: #c0392b;
  margin: 0;
  font-weight: 600;
}

/* ========================================
   ✅ 右键菜单样式
   ======================================== */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  min-width: 140px;
  overflow: hidden;
}
.context-menu-item {
  padding: 10px 18px;
  font-size: 13px;
  color: #3d2b1a;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.context-menu-item:hover {
  background: #fdf6ee;
}
.context-menu-danger {
  color: #c0392b;
}
.context-menu-danger:hover {
  background: #fef0f0;
}
</style>
