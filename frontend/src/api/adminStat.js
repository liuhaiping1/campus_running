import request from '@/utils/request'

/** 获取管理端统计概览 */
export function getOverview() {
  return request.get('/api/admin/stat/overview')
}
