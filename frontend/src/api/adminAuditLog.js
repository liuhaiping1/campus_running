import request from '@/utils/request'

/** 获取审计日志分页列表 */
export function getAuditLogs(params) {
  return request.get('/api/admin/audit-log/list', { params })
}
