import request from '@/utils/request'

/** 获取跑腿员认证审核列表 */
export function getAuthList(params) {
  return request.get('/api/admin/runner-auth/list', { params })
}

/** 审核跑腿员认证申请（通过/驳回） */
export function reviewAuth(id, data) {
  return request.post(`/api/admin/runner-auth/${id}/review`, data)
}
