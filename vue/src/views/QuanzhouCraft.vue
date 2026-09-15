<template>
  <!-- 非遗工艺页 - 1:1复刻 quanzhou_craft.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">非遗工艺</h2>

    <!-- 泉州文化子导航 -->
    <div class="quanzhou-nav">
      <ul>
        <li><router-link to="/quanzhou">泉州简介</router-link></li>
        <li><router-link to="/quanzhou/food">泉州美食</router-link></li>
        <li><router-link to="/quanzhou/craft" class="active-sub">非遗工艺</router-link></li>
      </ul>
    </div>

    <!-- 非遗工艺内容 -->
    <div class="quanzhou-content">
      <div class="craft-grid">
        <!-- 泉州木雕 -->
        <div class="craft-item">
          <img v-if="muoutouImg" :src="muoutouImg" alt="泉州木偶头" class="craft-image" />
          <div class="craft-info">
            <h3>泉州木偶头雕刻</h3>
            <p>泉州木偶头雕刻是国家级非物质文化遗产，以其精湛的雕刻技艺和丰富的艺术表现力闻名于世。木偶头造型生动，色彩鲜艳，融合了雕刻、彩绘等多种工艺。</p>
          </div>
        </div>

        <!-- 德化瓷雕 -->
        <div class="craft-item">
          <img v-if="baiciChajuImg" :src="baiciChajuImg" alt="德化白瓷茶具" class="craft-image" />
          <div class="craft-info">
            <h3>德化白瓷</h3>
            <p>德化白瓷是中国陶瓷艺术的瑰宝，以"白如雪、薄如纸、明如镜、声如磬"著称。德化白瓷茶具造型优美，釉面光洁，兼具实用与艺术价值。</p>
          </div>
        </div>

        <!-- 泉州剪纸 -->
        <div class="craft-item">
          <img v-if="shidiaoBaijianImg" :src="shidiaoBaijianImg" alt="惠安石雕摆件" class="craft-image" />
          <div class="craft-info">
            <h3>惠安石雕</h3>
            <p>惠安石雕是中国传统雕刻艺术的重要流派，以其精湛的技艺和独特的风格享誉海内外。惠安石雕作品题材广泛，造型生动，是闽南文化的重要代表。</p>
          </div>
        </div>

        <!-- 泉州提线木偶 -->
        <div class="craft-item">
          <img v-if="zhubianArtImg" :src="zhubianArtImg" alt="安溪竹编工艺品" class="craft-image" />
          <div class="craft-info">
            <h3>安溪竹编</h3>
            <p>安溪竹编工艺是闽南地区传统手工艺的代表，选用优质竹材，经过劈篾、编织、成型等多道工序制作而成。竹编工艺品造型别致，工艺精湛，具有浓郁的地方特色。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuanzhouCraft',
  data() {
    return {
      muoutouImg: this.$resolveImageUrl('muoutou.jpg'),
      baiciChajuImg: this.$resolveImageUrl('baici_chaju.jpg'),
      shidiaoBaijianImg: this.$resolveImageUrl('shidiao_baijian.jpg'),
      zhubianArtImg: this.$resolveImageUrl('zhubian_art.jpg')
    }
  }
}
</script>

<style scoped>
.quanzhou-nav a.active-sub {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: var(--white);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(212, 167, 106, 0.3);
}
</style>
