<template>
  <!-- 泉州美食页 - 1:1复刻 quanzhou_food.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">泉州美食</h2>

    <!-- 泉州文化子导航 -->
    <div class="quanzhou-nav">
      <ul>
        <li><router-link to="/quanzhou">泉州简介</router-link></li>
        <li><router-link to="/quanzhou/food" class="active-sub">泉州美食</router-link></li>
        <li><router-link to="/quanzhou/craft">非遗工艺</router-link></li>
      </ul>
    </div>

    <!-- 泉州美食内容 -->
    <div class="quanzhou-content">
      <div class="food-grid">
        <!-- 安溪铁观音 -->
        <div class="food-item">
          <img v-if="tieguanyinImg" :src="tieguanyinImg" alt="安溪铁观音" class="food-image" />
          <div class="food-info">
            <h3>安溪铁观音</h3>
            <p>安溪铁观音是中国十大名茶之一，产于福建省安溪县。其茶叶条索卷曲，色泽砂绿，冲泡后汤色金黄，香气馥郁持久，有"七泡有余香"之美誉。</p>
          </div>
        </div>

        <!-- 泉州绿豆饼 -->
        <div class="food-item">
          <img v-if="lvdoubingImg" :src="lvdoubingImg" alt="泉州绿豆饼" class="food-image" />
          <div class="food-info">
            <h3>泉州绿豆饼</h3>
            <p>泉州绿豆饼是闽南地区的传统糕点，选用优质绿豆为原料，配以白糖、食用油等精制而成。外皮酥脆，内馅香甜绵软，是泉州人喜爱的茶点和伴手礼。</p>
          </div>
        </div>

        <!-- 泉州海鲜 -->
        <div class="food-item">
          <img v-if="honggaoxieImg" :src="honggaoxieImg" alt="石狮红膏蟹" class="food-image" />
          <div class="food-info">
            <h3>泉州海鲜</h3>
            <p>泉州地处东南沿海，拥有丰富的海洋资源。泉州的海鲜以其新鲜、品种多样而闻名。石狮红膏蟹、深沪鱿鱼干、晋江巴浪鱼干等都是当地著名的海产品。</p>
          </div>
        </div>

        <!-- 泉州卤面 -->
        <div class="food-item">
          <img v-if="hutouMifenImg" :src="hutouMifenImg" alt="安溪湖头米粉" class="food-image" />
          <div class="food-info">
            <h3>安溪湖头米粉</h3>
            <p>安溪湖头米粉是泉州地区的传统名产，选用优质大米，采用传统工艺手工制作而成。米粉洁白细腻，口感爽滑，是闽南人喜爱的日常主食之一。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuanzhouFood',
  data() {
    return {
      tieguanyinImg: this.$resolveImageUrl('tieguanyin.jpg'),
      lvdoubingImg: this.$resolveImageUrl('lvdoubing.jpg'),
      honggaoxieImg: this.$resolveImageUrl('honggaoxie.jpg'),
      hutouMifenImg: this.$resolveImageUrl('hutou_mifen.jpg')
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
