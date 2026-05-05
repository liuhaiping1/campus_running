import request from '@/utils/request'

// 提交跑腿员认证申请
export function applyAuth(data) {
  return request.post('/api/runner-auth/apply', data)
}
