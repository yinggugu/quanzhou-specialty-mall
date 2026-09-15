<template>
  <!-- 联系我们页 - 1:1复刻 contact.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">联系我们</h2>

    <div class="contact">
      <!-- 联系信息 -->
      <div class="contact-info">
        <div class="contact-info-item">
          <h3>📞 联系电话</h3>
          <p>0595-12345678</p>
          <p>客服时间：周一至周日 9:00-18:00</p>
        </div>
        <div class="contact-info-item">
          <h3>📧 电子邮箱</h3>
        <p>service@example.com</p>
          <p>我们会在24小时内回复您的邮件</p>
        </div>
        <div class="contact-info-item">
          <h3>📍 公司地址</h3>
          <p>福建省泉州市丰泽区</p>
          <p>闽南文化创意产业园</p>
        </div>
        <div class="contact-info-item">
          <h3>🕐 营业时间</h3>
          <p>周一至周日</p>
          <p>上午 9:00 - 下午 6:00</p>
        </div>
      </div>

      <!-- 联系我们表单 -->
      <div class="contact-form">
        <h3 style="color: var(--secondary-color); margin-bottom: 25px; font-size: 1.3rem;">
          留言反馈
        </h3>
        <div class="form-group">
          <label for="name">姓名</label>
          <input
            type="text"
            id="name"
            v-model="form.name"
            class="form-control"
            required
            placeholder="请输入您的姓名"
          />
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            type="email"
            id="email"
            v-model="form.email"
            class="form-control"
            required
            placeholder="请输入您的邮箱"
          />
        </div>
        <div class="form-group">
          <label for="subject">主题</label>
          <input
            type="text"
            id="subject"
            v-model="form.subject"
            class="form-control"
            required
            placeholder="请输入留言主题"
          />
        </div>
        <div class="form-group">
          <label for="message">留言内容</label>
          <textarea
            id="message"
            v-model="form.message"
            class="form-control"
            rows="5"
            required
            placeholder="请输入您的留言内容"
          ></textarea>
        </div>
        <div class="form-actions">
          <button
            type="button"
            class="btn btn-primary"
            @click="handleSubmit"
            :disabled="submitting"
          >
            {{ submitting ? '提交中...' : '提交留言' }}
          </button>
        </div>
      </div>

      <!-- 社交媒体 -->
      <div class="contact-info" style="margin-top: 40px;">
        <div class="contact-info-item">
          <h3>💬 微信公众号</h3>
          <p>泉州特产商城</p>
          <p>关注获取最新优惠信息</p>
        </div>
        <div class="contact-info-item">
          <h3>🎵 抖音号</h3>
          <p>quanzhou_mall</p>
          <p>了解更多泉州特产推荐</p>
        </div>
        <div class="contact-info-item">
          <h3>📕 小红书</h3>
          <p>泉州特产推荐</p>
          <p>分享地道闽南好物</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Contact',

  data() {
    return {
      form: {
        name: '',
        email: '',
        subject: '',
        message: ''
      },
      submitting: false
    }
  },

  methods: {
    async handleSubmit() {
      // 表单验证
      if (!this.form.name.trim()) {
        this.$message.warning('请输入您的姓名')
        return
      }
      if (!this.form.email.trim()) {
        this.$message.warning('请输入您的邮箱')
        return
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(this.form.email)) {
        this.$message.warning('请输入有效的邮箱地址')
        return
      }
      if (!this.form.subject.trim()) {
        this.$message.warning('请输入留言主题')
        return
      }
      if (!this.form.message.trim()) {
        this.$message.warning('请输入留言内容')
        return
      }

      this.submitting = true
      try {
        // TODO: 对接后端留言接口
        this.$message.success('留言提交成功，感谢您的反馈！')
        // 重置表单
        this.form = { name: '', email: '', subject: '', message: '' }
      } catch (e) {
        this.$message.error('提交失败，请稍后重试')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
