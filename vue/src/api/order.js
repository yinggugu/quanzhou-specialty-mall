// 订单相关 API 接口
import request from './request'

/**
 * 获取订单列表（分页）
 * @param {Object} params - { page, pageSize }
 * @returns {Promise}
 */
export function getOrderList(params = {}) {
  return request({
    url: '/orders',
    method: 'get',
    params: {
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

/**
 * 获取订单详情
 * @param {number} id - 订单ID
 * @returns {Promise}
 */
export function getOrderDetail(id) {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

/**
 * 创建订单
 * @param {Object} data - { username, phone, address }
 * @returns {Promise}
 */
export function createOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

/**
 * 支付订单
 * @param {number} orderId - 订单ID
 * @returns {Promise}
 */
export function payOrder(orderId) {
  return request({
    url: `/orders/${orderId}/pay`,
    method: 'post'
  })
}

/**
 * 确认收货
 * @param {number} orderId - 订单ID
 * @returns {Promise}
 */
export function confirmReceipt(orderId) {
  return request({
    url: `/orders/${orderId}/confirm-receipt`,
    method: 'post'
  })
}

/**
 * 取消订单
 * @param {number} orderId - 订单ID
 * @returns {Promise}
 */
export function cancelOrder(orderId) {
  return request({
    url: `/orders/${orderId}/cancel`,
    method: 'post'
  })
}

/**
 * 获取订单中未评价的商品列表
 * @param {number} orderId - 订单ID
 * @returns {Promise}
 */
export function getUnreviewedItems(orderId) {
  return request({
    url: `/orders/${orderId}/unreviewed-items`,
    method: 'get'
  })
}
