<template>
  <div class="page-wrap">
    <!-- ✅ 改动1：删除「返回主页」按钮，由全局导航栏 AppHeader 替代 -->
    <div class="page-center">
      <div class="main-container">
        <!-- 左侧【我的订单】 -->
        <aside class="order-sidebar">
          <div class="sidebar-head">
            <h2 class="panel-title">我的订单</h2>
            <span class="order-count">共 {{ orderList.length }} 单</span>
          </div>
          <div class="order-list">
            <div
              v-for="item in orderList"
              :key="item.orderId"
              class="order-mini-card"
              @dblclick="sendOrderToChat(item)"
            >
              <div class="card-inner">
                <div class="img-placeholder">
                  <img v-if="getGoodsImage(item.goodsImg)" :src="getGoodsImage(item.goodsImg)" class="mini-goods-img" alt="商品图">
                </div>
                <div class="mini-order-info">
                  <p class="mini-order-id">{{ item.orderId }}</p>
                  <p class="mini-goods-name">{{ item.goodsTitle }}</p>
                  <div class="mini-bottom-row">
                    <p class="mini-price">¥{{ item.totalPrice }}</p>
                    <span class="mini-status">{{ item.statusText }}</span>
                  </div>
                </div>
              </div>
              <div class="order-card-actions">
                <a v-if="item.status === 0" class="action-btn action-pay" @click.stop="goPay(item)">去付款</a>
                <a v-if="item.status === 1" class="action-btn action-urge" @click.stop="urgeShip(item)">催发货</a>
                <a class="action-btn action-detail" :href="`/#/orders/${item.orderDbId}`" target="_blank" @click.stop>查看详情</a>
              </div>
            </div>
          </div>
        </aside>

        <div class="divider-line"></div>

        <!-- 右侧【客服咨询】 -->
        <section class="chat-main-panel">
          <div class="chat-header">
            <h2 class="panel-title">客服咨询</h2>
            <span class="service-tip">在线客服 · 9:00-18:00</span>
          </div>

          <div class="chat-scroll-wrap" ref="chatDom">
            <div v-for="(msg, idx) in chatMsgArr" :key="idx" class="bubble-row" :class="msg.isUser ? 'user-side' : 'service-side'">
              <div class="bubble-box">
                <template v-if="msg.msgType !== 'orderCard'">
                  <div class="bubble-txt">{{ msg.content }}</div>
                </template>
                <template v-else>
                  <div class="order-bubble-card">
                    <div class="card-img-placeholder">
                      <img v-if="getGoodsImage(msg.orderInfo.goodsImg)" :src="getGoodsImage(msg.orderInfo.goodsImg)" class="bubble-goods-img" alt="商品图">
                    </div>
                    <div class="bubble-goods-info">
                      <p class="bubble-goods-title">{{ msg.orderInfo.goodsTitle }}</p>
                      <p class="bubble-goods-price">¥{{ msg.orderInfo.totalPrice }}</p>
                      <p class="bubble-shop-name">泉州特产商城</p>
                    </div>
                  </div>
                </template>
                <div class="bubble-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>
          </div>

          <div class="chat-input-bar">
            <div class="input-wrapper">
              <input
                v-model="inputChatVal"
                class="chat-input"
                placeholder="请输入您的问题…"
                @keydown.enter="submitMsg"
              >
              <button class="send-btn" @click="submitMsg">发送</button>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import { getOrderList } from '@/api/order'

export default {
  name: 'UserServiceChat',
  data() {
    return {
      inputChatVal: '',
      // WebSocket
      ws: null,
      wsConnected: false,
      sessionId: null,
      userId: null,
      username: '',
      // 订单列表（从数据库加载）
      orderList: [],
      ordersLoading: false,
      // 聊天消息
      chatMsgArr: []
    }
  },
  mounted() {
    this.initUser()
    this.loadOrders()
    this.connectWebSocket()
  },
  beforeDestroy() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  },
  methods: {
    /* ============ 原有工具方法 ============ */
    getGoodsImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },
    formatTime(rawTime) {
      if (!rawTime) return ''
      const d = new Date(rawTime)
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    scrollToBottom() {
      const dom = this.$refs.chatDom
      if (dom) dom.scrollTop = dom.scrollHeight
    },

    /* ============ 初始化用户信息 ============ */
    initUser() {
      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (user) {
        this.userId = user.id
        this.username = user.username
      }
    },

    /* ============ 加载真实订单数据 ============ */
    async loadOrders() {
      if (!this.userId) return
      this.ordersLoading = true
      try {
        const res = await getOrderList({ page: 1, pageSize: 50 })
        if (res.code === 200 && res.data) {
          const list = res.data.list || res.data.records || []
          this.orderList = list.map(order => this.mapOrder(order))
        }
      } catch (e) {
        console.error('加载订单列表失败:', e)
      } finally {
        this.ordersLoading = false
      }
    },

    /* ============ 后端订单 → 页面展示格式 ============ */
    mapOrder(order) {
      // 商品图：取第一个商品图，从路径提取文件名
      let goodsImg = ''
      if (order.orderItems && order.orderItems.length > 0) {
        const img = order.orderItems[0].productImage || ''
        goodsImg = img.replace(/^.*[\\/]/, '')
      }
      // 商品标题：拼接所有商品名×数量
      let goodsTitle = ''
      if (order.orderItems && order.orderItems.length > 0) {
        goodsTitle = order.orderItems
          .map(item => `${item.productName}×${item.quantity}`)
          .join('、')
      }
      return {
        orderId: order.orderNo,
        orderDbId: order.id,
        goodsImg: goodsImg,
        goodsTitle: goodsTitle || order.orderNo,
        totalPrice: String(order.totalPrice),
        status: order.status,
        statusText: this.getStatusText(order.status)
      }
    },

    /* ============ 订单状态文本 ============ */
    getStatusText(status) {
      const map = { 0: '待付款', 1: '待发货', 2: '运输中', 3: '已完成', 4: '已取消', 5: '售后处理中', 6: '售后已完结' }
      return map[status] || '未知'
    },

    /* ============ WebSocket 连接 ============ */
    connectWebSocket() {
      const token = localStorage.getItem('token')
      if (!token || !this.userId) {
        // 无登录态时使用本地假数据演示
        this.chatMsgArr = [
          { isUser: false, content: '您好！欢迎咨询泉州特产商城客服，请问有什么可以帮您？', time: '2026-07-10T08:10:00.000+00:00', msgType: 'text' }
        ]
        return
      }
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = process.env.VUE_APP_WS_URL || 'localhost:8088'
      const url = `${protocol}//${host}/ws/chat?token=${encodeURIComponent(token)}`
      this.ws = new WebSocket(url)
      this.ws.onopen = () => {
        this.wsConnected = true
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
        // 3秒后自动重连
        setTimeout(() => {
          if (!this.wsConnected) this.connectWebSocket()
        }, 3000)
      }
      this.ws.onerror = () => {
        this.wsConnected = false
      }
    },

    /* ============ 处理 WebSocket 消息 ============ */
    handleWsMessage(data) {
      switch (data.type) {
        case 'sessionCreated':
          this.sessionId = data.sessionId
          // 加载历史消息
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type: 'loadHistory', sessionId: this.sessionId }))
          }
          break

        case 'historyMessages':
          if (data.messages && data.messages.length > 0) {
            this.chatMsgArr = data.messages.map(m => ({
              isUser: !m.isAdmin,
              msgType: m.msgType || 'text',
              content: (m.content || '').trim(),
              orderInfo: m.orderInfo || null,
              time: m.time || new Date().toISOString()
            }))
          } else {
            // 无历史消息显示默认欢迎语
            this.chatMsgArr = [{
              isUser: false,
              content: '您好！欢迎咨询泉州特产商城客服，请问有什么可以帮您？',
              time: new Date().toISOString(),
              msgType: 'text'
            }]
          }
          this.$nextTick(() => { this.scrollToBottom() })
          break

        case 'newMessage':
          // 只接收客服回复（isAdmin=true），自己发的消息已本地展示
          if (data.isAdmin) {
            this.chatMsgArr.push({
              isUser: false,
              msgType: data.msgType || 'text',
              content: (data.content || '').trim(),
              orderInfo: data.orderInfo || null,
              time: data.time || new Date().toISOString()
            })
            this.$nextTick(() => { this.scrollToBottom() })
          }
          break

        case 'sessionUpdated':
          // 客服标记已完成等状态同步
          if (data.status === '已完成') {
            this.$message.info('本次咨询已被客服标记为已完成')
          }
          break
      }
    },

    /* ============ 发送文本消息 ============ */
    submitMsg(e) {
      if (e) e.preventDefault()
      const content = this.inputChatVal.trim()
      if (!content) return

      const now = new Date().toISOString()
      // 本地先展示
      this.chatMsgArr.push({ isUser: true, content: content.trim(), msgType: 'text', time: now })
      this.inputChatVal = ''
      this.$nextTick(() => { this.scrollToBottom() })

      // 通过 WebSocket 发送（sessionId 可为 null，服务端自动创建）
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'message',
          sessionId: this.sessionId || 0,
          msgType: 'text',
          content: content
        }))
      }
    },

    /* ============ 发送订单卡片到聊天 ============ */
    sendOrderToChat(orderItem) {
      const now = new Date().toISOString()
      // 本地先展示
      this.chatMsgArr.push({ isUser: true, msgType: 'orderCard', orderInfo: { ...orderItem }, time: now })
      this.$nextTick(() => { this.scrollToBottom() })

      // 通过 WebSocket 发送
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'message',
          sessionId: this.sessionId || 0,
          msgType: 'orderCard',
          content: orderItem.goodsTitle,
          orderInfo: orderItem
        }))
      }
    },

    /* ============ 催发货 ============ */
    urgeShip(item) {
      const now = new Date().toISOString()
      const content = `请帮我催促订单 ${item.orderId}（${item.goodsTitle}）尽快发货，谢谢！`
      this.chatMsgArr.push({ isUser: true, content, msgType: 'text', time: now })
      this.$nextTick(() => { this.scrollToBottom() })

      // WebSocket 发送文字消息
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          type: 'message',
          sessionId: this.sessionId || 0,
          msgType: 'text',
          content: content
        }))
      }
      // 接着发送订单卡片
      this.sendOrderToChat(item)
    },

    /* ============ 去付款 ============ */
    goPay(item) {
      this.$message.info('跳转支付页面：' + item.orderId)
    }
  }
}
</script>

<style scoped>
/* ========================================
   全局 — 适配标准布局（含全局导航栏）
   ✅ 改动2：移除 min-height:100vh，调整为自适应高度
   ======================================== */
.page-wrap {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 8px 24px;
  box-sizing: border-box;
  gap: 14px;
}

/* ✅ 改动3：聊天面板高度从固定640px改为响应式，适配导航栏 */
.page-center {
  width: 1080px;
  max-width: 96vw;
  height: calc(100vh - 160px); /* 导航栏 + main-content padding + 底部留白 */
  min-height: 480px;
  background: #fff;
  border-radius: 18px;
  box-shadow:
    0 2px 8px rgba(92, 51, 23, 0.04),
    0 8px 32px rgba(92, 51, 23, 0.06),
    0 20px 64px rgba(92, 51, 23, 0.04);
  border: 1px solid rgba(180, 150, 120, 0.12);
  overflow: hidden;
}

/* ========================================
   左右布局
   ======================================== */
.main-container {
  display: flex;
  align-items: stretch;
  padding: 32px 36px;
  height: 100%;
  box-sizing: border-box;
}
.divider-line {
  width: 1px;
  background: #ede6dc;
  margin: 0 28px;
  flex-shrink: 0;
}

/* ========================================
   左侧订单栏
   ======================================== */
.order-sidebar {
  width: 300px;
  height: 100%;
  overflow-y: auto;
  flex-shrink: 0;
  padding-right: 4px;
}
.sidebar-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}
.sidebar-head .panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #3d2b1a;
  padding-bottom: 8px;
  border-bottom: 2px solid #d4bc9f;
  flex: 1;
}
.order-count {
  font-size: 12px;
  color: #b0957a;
  margin-left: 10px;
  flex-shrink: 0;
}

/* ---- 订单卡片 ---- */
.order-mini-card {
  border-radius: 10px;
  margin-bottom: -1px;
  border: 1px solid #f0e8dc;
  transition: all 0.25s ease;
  background: #fff;
  overflow: hidden;
}
.order-mini-card:hover {
  background: #faf7f3;
  border-color: #d4bc9f;
  box-shadow: 0 3px 12px rgba(92, 51, 23, 0.06);
  transform: translateY(-1px);
}
.card-inner {
  display: flex;
  gap: 12px;
  padding: 12px 10px;
}

/* ---- 图片 ---- */
.img-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #faf6f0;
}
.mini-goods-img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
}

/* ---- 订单信息 ---- */
.mini-order-info {
  flex: 1;
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.mini-order-id {
  color: #b0957a;
  margin: 0;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-goods-name {
  color: #3d2b1a;
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.mini-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mini-price {
  color: #c0392b;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}
.mini-status {
  font-size: 11px;
  color: #b0957a;
}

/* ---- hover 操作按钮 ---- */
.order-card-actions {
  width: 100%;
  display: flex;
  gap: 8px;
  padding: 0 10px;
  opacity: 0;
  max-height: 0;
  overflow: hidden;
  transition: all 0.25s ease;
}
.order-mini-card:hover .order-card-actions {
  opacity: 1;
  max-height: 44px;
  padding: 0 10px 10px;
}
.action-btn {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  font-size: 12px;
  border-radius: 6px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}
.action-btn:hover {
  transform: translateY(-1px);
}
.action-pay {
  background: #d2a679;
  color: #fff;
}
.action-pay:hover {
  background: #c19668;
}
.action-urge {
  background: #5c3317;
  color: #fff;
}
.action-urge:hover {
  background: #3d2b1a;
}
.action-detail {
  background: #fff;
  color: #5c3317;
  border: 1.5px solid #d4bc9f;
}
.action-detail:hover {
  background: #5c3317;
  color: #fff;
  border-color: #5c3317;
}

/* ========================================
   右侧聊天
   ======================================== */
.chat-main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}
.chat-header .panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #3d2b1a;
  padding-bottom: 8px;
  border-bottom: 2px solid #d4bc9f;
  flex: 1;
}
.service-tip {
  font-size: 12px;
  color: #b0957a;
  margin-left: 10px;
  flex-shrink: 0;
}

/* ---- 气泡区 ---- */
.chat-scroll-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px;
}
.bubble-row {
  margin-bottom: 16px;
  display: flex;
}
.service-side {
  justify-content: flex-start;
}
.user-side {
  justify-content: flex-end;
}
.bubble-box {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}
.user-side .bubble-box {
  align-items: flex-end;
}
.service-side .bubble-box {
  align-items: flex-start;
}

/* ---- 文字气泡 ---- */
.bubble-txt {
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.4;
  word-break: break-word;
  width: fit-content;
  max-width: 100%;
}
.service-side .bubble-txt {
  background: #f7f4f1;
  color: #3d2b1a;
  border-bottom-left-radius: 6px;
}
.user-side .bubble-txt {
  background: #5c3317;
  color: #fff;
  border-bottom-right-radius: 6px;
}

/* ---- 订单卡片气泡 ---- */
.order-bubble-card {
  display: flex;
  gap: 10px;
  background: #fff;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #ede6dc;
  width: 260px;
  box-sizing: border-box;
}
.card-img-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  flex-shrink: 0;
  overflow: hidden;
  background: #faf6f0;
}
.bubble-goods-img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
}
.bubble-goods-info {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.bubble-goods-title {
  font-size: 13px;
  color: #3d2b1a;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bubble-goods-price {
  font-size: 15px;
  color: #c0392b;
  margin: 2px 0;
  font-weight: 600;
}
.bubble-shop-name {
  font-size: 11px;
  color: #b0957a;
  margin: 0;
}

/* ---- 时间 ---- */
.bubble-time {
  font-size: 11px;
  color: #c4b49e;
  margin-top: 4px;
}
.service-side .bubble-time {
  text-align: left;
}
.user-side .bubble-time {
  text-align: right;
}

/* ========================================
   输入栏
   ======================================== */
.chat-input-bar {
  margin-top: 14px;
  flex-shrink: 0;
}
.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: center;
  background: #faf7f3;
  border-radius: 22px;
  padding: 5px 5px 5px 16px;
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
  line-height: 36px;
  font-family: inherit;
}
.chat-input::placeholder {
  color: #c4b49e;
}
.send-btn {
  background: #5c3317;
  border: none;
  color: #fff;
  border-radius: 18px;
  padding: 8px 20px;
  font-size: 13.5px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
  font-family: inherit;
}
.send-btn:hover {
  background: #3d2b1a;
}
.send-btn:active {
  background: #2a1a0e;
}

/* ========================================
   滚动条
   ======================================== */
.order-sidebar,
.chat-scroll-wrap {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.order-sidebar::-webkit-scrollbar,
.chat-scroll-wrap::-webkit-scrollbar {
  display: none;
}
</style>
