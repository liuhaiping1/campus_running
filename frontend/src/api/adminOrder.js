import request from '@/utils/request'

/** 获取全量订单分页（管理端） */
export function getAllOrders(params) {
  return request.get('/api/admin/order/list', { params })
}

/** 获取订单详情（管理端） */
export function getAdminOrderDetail(id) {
  return request.get(`/api/admin/order/${id}`)
}
