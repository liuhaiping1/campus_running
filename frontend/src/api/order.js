import request from '@/utils/request'

// 创建跑腿订单
export function createOrder(data) {
  return request.post('/api/order', data)
}

// 发起支付宝支付，返回支付表单 HTML
export function payOrder(id) {
  return request.post(`/api/pay/${id}`)
}

// 查询我的订单列表（含 orderStatus / payStatus 筛选）
export function getMyOrders(params) {
  return request.get('/api/order', { params })
}

// 查询订单详情（含状态流转日志）
export function getOrderDetail(id) {
  return request.get(`/api/order/${id}`)
}

// 查询任务大厅（跑腿员可接订单）
export function getHallOrders(params) {
  return request.get('/api/order/hall', { params })
}

// 跑腿员接单
export function acceptOrder(id) {
  return request.post(`/api/order/${id}/accept`)
}

// 跑腿员确认已联系发布人
export function contactOrder(id) {
  return request.post(`/api/order/${id}/contact`)
}

// 跑腿员确认已取件
export function pickupOrder(id) {
  return request.post(`/api/order/${id}/pickup`)
}

// 跑腿员确认已送达
export function deliverOrder(id) {
  return request.post(`/api/order/${id}/deliver`)
}

// 发布人确认订单完成
export function completeOrder(id) {
  return request.post(`/api/order/${id}/complete`)
}

// 取消订单（需传 cancelReason）
export function cancelOrder(id, data) {
  return request.post(`/api/order/${id}/cancel`, data)
}

// 提交评价
export function submitEvaluation(data) {
  return request.post('/api/evaluation', data)
}

// 提交申诉
export function submitAppeal(data) {
  return request.post('/api/appeal', data)
}
