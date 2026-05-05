import request from '@/utils/request'

/**
 * 上传文件到服务器
 *
 * @param {File} file - 要上传的文件对象
 * @returns {Promise<Object>} 返回 { originalName, fileName, fileUrl, fileSize, contentType }
 */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  // Axios 会自动设置 Content-Type: multipart/form-data 及 boundary，不要手动设置
  return request.post('/api/file/upload', formData)
}
