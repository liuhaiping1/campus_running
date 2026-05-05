import request from '@/utils/request'

/** 获取退款记录分页 */
export function getRefundList(params) {
  return request.get('/api/admin/refund/list', { params })
}

/** 审核退款（通过/拒绝） */
export function approveRefund(id, data) {
  return request.post(`/api/admin/refund/${id}/approve`, data)
}
