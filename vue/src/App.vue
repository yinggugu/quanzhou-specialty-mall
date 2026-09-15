<template>
  <div id="app">
    <!-- 后台管理页面、客服页面使用全屏布局，不显示商城头部和底部 -->
    <template v-if="isFullPageRoute">
      <router-view />
    </template>

    <!-- 前台商城页面：显示公共头部 + 内容 + 底部 -->
    <template v-else>
      <AppHeader />
      <main class="main-content">
        <router-view />
      </main>
      <AppFooter />
      <!-- 回到顶部按钮 -->
      <button class="footer-back-to-top" id="backToTopBtn" @click="scrollToTop" title="回到顶部">
        ↑
      </button>
    </template>

    <!-- ✅ AI 助手浮动按钮 -->
    <div class="ai-float-btn" @click="$router.push('/ai')" title="AI小助手">
      <span class="ai-tooltip">AI小助手</span>
      <span class="ai-icon">🤖</span>
    </div>
  </div>
</template>

<script>
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

export default {
  name: 'App',
  components: {
    AppHeader,
    AppFooter
  },

  computed: {
    // 判断是否为全屏路由（后台管理、客服咨询等不需要商城头尾的页面）
    isFullPageRoute() {
      // ✅ /service 路由使用标准布局（含全局导航栏），仅 /admin 使用全屏布局
      return this.$route.path.startsWith('/admin')
    }
  },

  mounted() {
    this.initBackToTop()
  },

  methods: {
    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    initBackToTop() {
      window.addEventListener('scroll', () => {
        const btn = document.getElementById('backToTopBtn')
        if (!btn) return
        if (window.pageYOffset > 300) {
          btn.classList.add('visible')
        } else {
          btn.classList.remove('visible')
        }
      })
    }
  }
}
</script>

<style scoped>
.ai-float-btn {
  position: fixed; right: 24px; bottom: 100px;
  width: 52px; height: 52px; background: #fff; border-radius: 14px;
  box-shadow: 0 2px 12px rgba(92,51,23,0.12); z-index: 999;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: box-shadow .2s;
}
.ai-float-btn:hover { box-shadow: 0 4px 20px rgba(92,51,23,0.2); }
.ai-icon { font-size: 26px; line-height: 1; }
.ai-tooltip {
  position: absolute; right: 64px; white-space: nowrap;
  background: #4a3322; color: #fff; font-size: 13px;
  padding: 6px 14px; border-radius: 6px;
  opacity: 0; pointer-events: none; transition: opacity .2s;
}
.ai-float-btn:hover .ai-tooltip { opacity: 1; }
</style>
