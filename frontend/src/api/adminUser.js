import request from '@/utils/request'

/** 查询用户列表 */
export function getAdminUserList(params) {
  return request.get('/api/admin/user/list', { params })
}

/** 查询用户详情 */
export function getAdminUserDetail(id) {
  return request.get(`/api/admin/user/${id}`)
}

/** 修改用户资料 */
export function updateAdminUser(id, data) {
  return request.put(`/api/admin/user/${id}`, data)
}

/** 变更用户状态 */
export function updateAdminUserStatus(id, data) {
  return request.put(`/api/admin/user/${id}/status`, data)
}
