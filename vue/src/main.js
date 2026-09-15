// Vue2 入口文件 - 泉州特产商城
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import { resolveImageUrl } from './utils/image'
import 'element-ui/lib/theme-chalk/index.css'

// 引入全部原有CSS样式（1:1复刻JSP视觉效果）
import './assets/css/base.css'
import './assets/css/layout.css'
import './assets/css/components.css'
import './assets/css/header.css'
import './assets/css/footer.css'
import './assets/css/pages.css'
import './assets/css/animations.css'
import './assets/css/admin.css'

Vue.use(ElementUI)

Vue.config.productionTip = false
Vue.prototype.$resolveImageUrl = resolveImageUrl

// ============================================================
// ✅ 全局提示弹窗统一改造：2秒自动关闭 + 手动关闭按钮
//    不影响原有聊天、会话、双表鉴权、WebSocket 业务代码
// ============================================================
const _Message = ElementUI.Message
const msgTypes = ['success', 'warning', 'info', 'error']

// 包装每个类型的快捷方法：强制 duration=1000 + showClose=true
msgTypes.forEach(type => {
  const original = _Message[type]
  _Message[type] = function (options) {
    if (typeof options === 'string') {
      options = { message: options }
    }
    return original.call(_Message, {
      duration: 1000,
      showClose: true,
      ...options
    })
  }
})

// 包装通用 Message 方法（对象调用形式）
const originalMsg = _Message.bind(_Message)
Vue.prototype.$message = function (options) {
  if (typeof options === 'string') {
    options = { message: options }
  }
  return originalMsg({
    duration: 1000,
    showClose: true,
    ...options
  })
}
// 保留快捷方法引用
msgTypes.forEach(type => {
  Vue.prototype.$message[type] = _Message[type]
})
// ============================================================

// 全局过滤器：ISO时间格式转换为 yyyy-MM-dd HH:mm:ss
Vue.filter('formatTime', function (value) {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
