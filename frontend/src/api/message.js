import request from '@/utils/request'

// 分页查询当前用户消息
export function getMessageList(params) {
  return request.get('/api/message/list', { params })
}

// 标记单条消息已读
export function readMessage(id) {
  return request.post(`/api/message/${id}/read`)
}

// 标记全部消息已读
export function readAllMessage() {
  return request.post('/api/message/read-all')
}
