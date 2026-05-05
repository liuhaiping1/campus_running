import request from '@/utils/request'

/** 获取后台分类分页列表 */
export function getAdminCategoryList(params) {
  return request.get('/api/admin/category/list', { params })
}

/** 新增分类 */
export function createCategory(data) {
  return request.post('/api/admin/category', data)
}

/** 修改分类 */
export function updateCategory(id, data) {
  return request.put(`/api/admin/category/${id}`, data)
}

/** 删除分类（逻辑删除） */
export function deleteCategory(id) {
  return request.delete(`/api/admin/category/${id}`)
}
