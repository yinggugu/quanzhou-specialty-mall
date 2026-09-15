<template>
  <!--
    全局文字头像组件
    - 有图片：展示原图
    - 无图片：提取用户名首字/首字母，暖棕随机底色
  -->
  <div class="user-avatar" :style="avatarStyle" :title="username">
    <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" :alt="username" />
    <span v-else class="avatar-text">{{ avatarChar }}</span>
  </div>
</template>

<script>
// 暖棕浅色系背景池
const BG_COLORS = [
  '#f5ece0', '#ede0ce', '#faf3e8', '#fdf6ee', '#f8efe2',
  '#f0e4d4', '#faf0e0', '#eee3d3', '#fdf3e5', '#f7ede0'
]

export default {
  name: 'UserAvatar',
  props: {
    username: { type: String, default: '' },
    avatarUrl: { type: String, default: '' },
    size: { type: Number, default: 38 }
  },
  computed: {
    avatarChar() {
      if (!this.username) return '?'
      // 优先取第一个汉字，否则取首字母大写
      const first = this.username.trim().charAt(0)
      return first || '?'
    },
    bgColor() {
      if (!this.username) return BG_COLORS[0]
      const idx = this.username.charCodeAt(0) % BG_COLORS.length
      return BG_COLORS[idx]
    },
    avatarStyle() {
      return {
        width: this.size + 'px',
        height: this.size + 'px',
        borderRadius: '50%',
        backgroundColor: this.bgColor,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: '0',
        overflow: 'hidden',
        fontSize: (this.size * 0.4) + 'px',
        fontWeight: '600',
        color: '#5c3317'
      }
    }
  }
}
</script>

<style scoped>
.user-avatar { user-select: none; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }
.avatar-text { line-height: 1; }
</style>
