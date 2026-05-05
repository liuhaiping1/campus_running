import request from '@/utils/request'

export function login(data) {
  return request.post('/api/auth/login', data)
}

export function register(data) {
  return request.post('/api/auth/register', data)
}

export function getRunnerAuthPage(params) {
  return request.get('/api/admin/runner-auth/list', { params })
}

export function reviewRunnerAuth(id, data) {
  return request.post(`/api/admin/runner-auth/${id}/review`, data)
}