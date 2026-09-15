// axios 统一封装 - 适配 Spring Boot 后端接口
import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  // baseURL 通过 vue.config.js 代理到 Spring Boot 后端
  baseURL: process.env.VUE_APP_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 JWT Token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理响应和错误
service.interceptors.response.use(
  response => {
    const res = response.data

    // 如果返回的是文件流或其他非JSON格式，直接返回
    if (typeof res !== 'object') {
      return response
    }

    // 业务错误处理
    if (res.code && res.code !== 200) {
      Message({
        message: res.message || '请求失败',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('响应错误:', error)

    if (error.response) {
      const { status } = error.response

      switch (status) {
        case 401:
          // Token 过期或未授权
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          Message({
            message: '登录已过期，请重新登录',
            type: 'warning',
            duration: 3000
          })
          router.push({ name: 'Login', query: { redirect: router.currentRoute.fullPath } })
          break
        case 403:
          Message({
            message: '没有权限访问',
            type: 'error',
            duration: 3000
          })
          break
        case 404:
          Message({
            message: '请求的资源不存在',
            type: 'error',
            duration: 3000
          })
          break
        case 500:
          Message({
            message: '服务器内部错误',
            type: 'error',
            duration: 3000
          })
          break
        default:
          Message({
            message: error.response.data?.message || `请求失败 (${status})`,
            type: 'error',
            duration: 3000
          })
      }
    } else if (error.message.includes('timeout')) {
      Message({
        message: '请求超时，请重试',
        type: 'error',
        duration: 3000
      })
    } else {
      Message({
        message: '网络连接失败，请检查网络',
        type: 'error',
        duration: 3000
      })
    }

    return Promise.reject(error)
  }
)

export default service
