// 商品相关 API 接口
import request from './request'

/**
 * 获取商品列表（分页）
 * @param {Object} params - { page, pageSize, categoryId }
 * @returns {Promise}
 */
export function getProductList(params = {}) {
  return request({
    url: '/products',
    method: 'get',
    params: {
      page: params.page || 1,
      pageSize: params.pageSize || 12,
      categoryId: params.categoryId || undefined,
      goodsName: params.goodsName || undefined   // ✅ 模糊搜索关键词
    }
  })
}

/**
 * 获取推荐商品（首页用）
 * @param {number} limit - 获取数量
 * @returns {Promise}
 */
export function getRecommendedProducts(limit = 3) {
  return request({
    url: '/products/recommended',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取商品详情
 * @param {number} id - 商品ID
 * @returns {Promise}
 */
export function getProductDetail(id) {
  return request({
    url: `/products/${id}`,
    method: 'get'
  })
}

/**
 * 获取分类列表
 * @returns {Promise}
 */
export function getCategories() {
  return request({
    url: '/products/categories',
    method: 'get'
  })
}

// ============ 评价相关 API ============

/** 提交评价 */
export function addComment(data) {
  return request({ url: '/comment/add', method: 'post', data })
}

/** 获取商品评价列表 */
export function getCommentList(params) {
  return request({ url: '/comment/list', method: 'get', params })
}

/** 点赞评价 */
export function likeComment(commentId) {
  return request({ url: '/comment/like', method: 'post', data: { commentId } })
}

/** 管理员获取评价列表 */
export function adminCommentList(params) {
  return request({ url: '/admin/comment/list', method: 'get', params })
}

/** 管理员隐藏评价 */
export function hideComment(id) {
  return request({ url: '/admin/comment/hide', method: 'put', data: { id } })
}

/** 管理员回复评价 */
export function replyComment(id, reply) {
  return request({ url: '/admin/comment/reply', method: 'put', data: { id, reply } })
}

/** 敏感词管理 */
export function getSensitiveWords() {
  return request({ url: '/admin/comment/words', method: 'get' })
}
export function addSensitiveWord(word) {
  return request({ url: '/admin/comment/words', method: 'post', data: { word } })
}
export function deleteSensitiveWord(id) {
  return request({ url: `/admin/comment/words/${id}`, method: 'delete' })
}
