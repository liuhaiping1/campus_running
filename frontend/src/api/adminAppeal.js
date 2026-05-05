import request from '@/utils/request'

/** 获取申诉分页列表 */
export function getAppealList(params) {
  return request.get('/api/admin/appeal/list', { params })
}

/** 处理申诉（成立/驳回/关闭） */
export function handleAppeal(id, data) {
  return request.post(`/api/admin/appeal/${id}/handle`, data)
}
