<template>
  <!-- 泉州简介页 - 1:1复刻 quanzhou_intro.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">泉州简介</h2>

    <!-- 泉州文化子导航 -->
    <div class="quanzhou-nav">
      <ul>
        <li><router-link to="/quanzhou" exact class="active-sub">泉州简介</router-link></li>
        <li><router-link to="/quanzhou/food">泉州美食</router-link></li>
        <li><router-link to="/quanzhou/craft">非遗工艺</router-link></li>
      </ul>
    </div>

    <!-- 泉州简介内容 -->
    <div class="quanzhou-content">
      <div class="quanzhou-intro">
        <img
          v-if="quanzhou1Img"
          :src="quanzhou1Img"
          alt="泉州风光"
          class="quanzhou-intro-image"
        />
        <div class="quanzhou-intro-text">
          <h3>泉州 - 海上丝绸之路起点</h3>
          <p>泉州，古称刺桐，地处福建省东南部，是一座历史悠久、文化底蕴深厚的城市。作为海上丝绸之路的起点，泉州在宋元时期曾是世界最大的港口之一，与世界上100多个国家和地区有贸易往来。</p>
          <p>泉州是国家历史文化名城，拥有众多名胜古迹，如开元寺、清净寺、天后宫、洛阳桥等。这些文化遗产见证了泉州的辉煌历史和多元文化的融合。</p>
          <p>泉州的文化丰富多彩，包括闽南语、南音、木偶戏、梨园戏等非物质文化遗产。此外，泉州还有众多美食特产，如安溪铁观音、泉州绿豆饼、石狮牛肉丸等，深受游客和市民喜爱。</p>
          <p>如今，泉州正以开放的姿态走向世界，传承和弘扬闽南文化，同时积极发展现代产业，成为一座充满活力的现代化城市。</p>
        </div>
      </div>

      <!-- 泉州风光图片画廊 -->
      <div class="quanzhou-gallery">
        <h3>泉州风光</h3>
        <div class="gallery-grid">
          <div class="gallery-item">
            <img v-if="quanzhou2Img" :src="quanzhou2Img" alt="泉州古建筑" class="gallery-image" />
            <div class="gallery-caption">古建筑群</div>
          </div>
          <div class="gallery-item">
            <img v-if="quanzhou3Img" :src="quanzhou3Img" alt="泉州文化" class="gallery-image" />
            <div class="gallery-caption">文化遗产</div>
          </div>
          <div class="gallery-item">
            <img v-if="tieguanyinImg" :src="tieguanyinImg" alt="安溪铁观音" class="gallery-image" />
            <div class="gallery-caption">安溪铁观音</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuanzhouIntro',
  data() {
    return {
      quanzhou1Img: this.$resolveImageUrl('quanzhou-1.jpg'),
      quanzhou2Img: this.$resolveImageUrl('quanzhou-2.jpg'),
      quanzhou3Img: this.$resolveImageUrl('quanzhou-3.jpg'),
      tieguanyinImg: this.$resolveImageUrl('tieguanyin.jpg')
    }
  }
}
</script>

<style scoped>
/* 子导航 active 样式与原 JSP .quanzhou-nav a.active 一致 */
.quanzhou-nav a.active-sub {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: var(--white);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(212, 167, 106, 0.3);
}
</style>
