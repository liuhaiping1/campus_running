import request from '@/utils/request'

/** 获取后台公告分页列表 */
export function getAdminNoticeList(params) {
  return request.get('/api/admin/notice/list', { params })
}

/** 新增公告 */
export function createNotice(data) {
  return request.post('/api/admin/notice', data)
}

/** 修改公告 */
export function updateNotice(id, data) {
  return request.put(`/api/admin/notice/${id}`, data)
}

/** 变更公告状态（发布/下架/改草稿） */
export function changeNoticeStatus(id, data) {
  return request.post(`/api/admin/notice/${id}/status`, data)
}
