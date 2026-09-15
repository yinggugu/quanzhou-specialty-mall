<template>
  <!-- 底部页脚 - 1:1复刻原JSP footer结构 -->
  <footer>
    <div class="container">
      <!-- 四栏页脚内容 -->
      <div class="footer-content">
        <!-- 第一栏：关于商城 -->
        <div class="footer-section">
          <h3>泉州特产商城</h3>
          <p>专注于推广泉州特产，传承闽南文化</p>
          <p>联系电话：0595-12345678</p>
        <p>邮箱：service@example.com</p>
        </div>

        <!-- 第二栏：快速链接 -->
        <div class="footer-section">
          <h3>快速链接</h3>
          <ul>
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/products">商品列表</router-link></li>
            <li><router-link to="/quanzhou">泉州文化</router-link></li>
            <li><router-link to="/contact">联系我们</router-link></li>
          </ul>
        </div>

        <!-- 第三栏：文化展示 -->
        <div class="footer-section">
          <h3>文化展示</h3>
          <ul>
            <li><router-link to="/quanzhou">泉州简介</router-link></li>
            <li><router-link to="/quanzhou/food">泉州美食</router-link></li>
            <li><router-link to="/quanzhou/craft">非遗工艺</router-link></li>
          </ul>
        </div>

        <!-- 第四栏：关注我们 -->
        <div class="footer-section">
          <h3>关注我们</h3>
          <p>微信公众号：泉州特产商城</p>
          <p>抖音号：quanzhou_mall</p>
          <p>小红书：泉州特产推荐</p>
        </div>
      </div>

      <!-- 版权信息 -->
      <div class="footer-bottom">
        <p>&copy; 2024 泉州特产商城. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'AppFooter'
}
</script>
