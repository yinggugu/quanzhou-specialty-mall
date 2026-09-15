const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  // 开发服务器配置
  devServer: {
    port: 3000,
    // 代理 Spring Boot 后端 API
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  // 静态资源路径配置
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  // 生产环境不生成 source map
  productionSourceMap: false
})
