import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

/**
 * 文件上传组合式函数
 * <p>
 * 封装 el-upload 自定义 http-request 所需的校验、上传、加载状态，
 * 避免在多页面重复相同的上传逻辑。
 * </p>
 *
 * @param {Object} options - 配置项
 * @param {string[]} [options.allowedTypes] - 允许的 MIME 类型列表，默认 ['image/jpeg','image/png','image/webp']
 * @param {number} [options.maxSizeMb=5] - 最大文件大小（MB）
 * @param {string} [options.typeErrorMsg] - 文件类型错误提示
 * @param {string} [options.sizeErrorMsg] - 文件大小超限提示
 * @param {string} [options.successMsg='上传成功'] - 成功提示
 * @param {Function} options.onSuccess - 上传成功回调，接收 { fileUrl, fileName, ... } 结果对象
 * @returns {{ uploading: Ref<boolean>, handleUpload: Function }}
 */
export function useFileUpload(options = {}) {
  const {
    allowedTypes = ['image/jpeg', 'image/png', 'image/webp'],
    maxSizeMb = 5,
    typeErrorMsg = '不支持的文件类型',
    sizeErrorMsg = '文件大小不能超过 ' + maxSizeMb + 'MB',
    successMsg = '上传成功',
    onSuccess
  } = options

  const uploading = ref(false)

  /**
   * el-upload http-request 自定义上传方法
   */
  async function handleUpload(uploadOptions) {
    const file = uploadOptions.file
    if (!allowedTypes.includes(file.type)) {
      ElMessage.error(typeErrorMsg)
      return
    }
    if (file.size > maxSizeMb * 1024 * 1024) {
      ElMessage.error(sizeErrorMsg)
      return
    }
    uploading.value = true
    try {
      const result = await uploadFile(file)
      if (onSuccess) onSuccess(result)
      ElMessage.success(successMsg)
    } catch {
      // 错误已在 request 拦截器中统一处理
    } finally {
      uploading.value = false
    }
  }

  return { uploading, handleUpload }
}
