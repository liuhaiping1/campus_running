import request from '@/utils/request'

export function getDict(dictType) {
  return request.get(`/api/system/dict/${dictType}`)
}

export function getNoticeList(params) {
  return request.get('/api/notice/list', { params })
}

export function getCategoryList() {
  return request.get('/api/category/list')
}