// 认证相关 API 接口
import request from './request'

/**
 * 前台用户登录 (POST /api/auth/userLogin)
 * @param {Object} data - { username, password }
 */
export function userLogin(data) {
  return request({
    url: '/auth/userLogin',
    method: 'post',
    data
  })
}

/**
 * 后台管理员登录 (POST /api/auth/adminLogin)
 * @param {Object} data - { username, password }
 */
export function adminLogin(data) {
  return request({
    url: '/auth/adminLogin',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

// ============ JWT 工具函数 ============

/**
 * 从 token 字符串解析用户信息（仅解析 payload，不验证签名）
 * @param {string} token - JWT token
 * @returns {{ userId: number, username: string, type: string } | null}
 */
export function parseToken(token) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = JSON.parse(atob(parts[1]))
    return {
      userId: payload.userId,
      username: payload.username,
      type: payload.type // "user" 或 "admin"
    }
  } catch (e) {
    return null
  }
}
