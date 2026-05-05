import request from '@/utils/request'

// 查询当前用户地址列表
export function listAddress() {
  return request.get('/api/address')
}

// 新增地址
export function createAddress(data) {
  return request.post('/api/address', data)
}

// 修改地址
export function updateAddress(id, data) {
  return request.put(`/api/address/${id}`, data)
}

// 删除地址
export function deleteAddress(id) {
  return request.delete(`/api/address/${id}`)
}

// 设置默认地址
export function setDefaultAddress(id) {
  return request.post(`/api/address/${id}/default`)
}
