import request from '@/utils/request'

/** 查询当前登录用户资料 */
export function getUserProfile() {
  return request.get('/api/user/profile')
}

/** 修改当前登录用户资料 */
export function updateUserProfile(data) {
  return request.put('/api/user/profile', data)
}

/** 修改密码 */
export function changePassword(data) {
  return request.post('/api/user/password/change', data)
}

/** 学生个人中心概览 */
export function getUserCenterOverview() {
  return request.get('/api/user/center/overview')
}

/** 跑腿员个人中心概览 */
export function getRunnerCenterOverview() {
  return request.get('/api/runner/center/overview')
}

/** 跑腿员认证信息查询 */
export function getRunnerAuthProfile() {
  return request.get('/api/runner/profile/auth')
}
