// 购物车相关 API 接口
import request from './request'

/**
 * 获取购物车内容
 * @returns {Promise}
 */
export function getCart() {
  return request({
    url: '/cart',
    method: 'get'
  })
}

/**
 * 添加商品到购物车
 * @param {Object} data - { productId, quantity }
 * @returns {Promise}
 */
export function addToCart(data) {
  return request({
    url: '/cart/items',
    method: 'post',
    data
  })
}

/**
 * 更新购物车商品数量
 * @param {number} productId - 商品ID
 * @param {number} quantity - 新数量
 * @returns {Promise}
 */
export function updateCartItem(productId, quantity) {
  return request({
    url: `/cart/items/${productId}`,
    method: 'patch',
    data: { quantity }
  })
}

/**
 * 从购物车删除商品
 * @param {number} productId - 商品ID
 * @returns {Promise}
 */
export function deleteCartItem(productId) {
  return request({
    url: `/cart/items/${productId}`,
    method: 'delete'
  })
}

/**
 * 清空购物车
 * @returns {Promise}
 */
export function clearCart() {
  return request({
    url: '/cart',
    method: 'delete'
  })
}

/**
 * 获取结算信息
 * @returns {Promise}
 */
export function getCheckoutInfo() {
  return request({
    url: '/cart/checkout',
    method: 'get'
  })
}
