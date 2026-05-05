import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useDictStore = defineStore('dict', () => {
  const dictData = ref({})

  async function loadDict(dictType) {
    if (dictData.value[dictType]) {
      return dictData.value[dictType]
    }
    try {
      const data = await request.get(`/api/system/dict/${dictType}`)
      dictData.value[dictType] = data
      return data
    } catch {
      return []
    }
  }

  function getDictLabel(dictType, value) {
    const list = dictData.value[dictType] || []
    const item = list.find(d => d.dictValue === String(value) || d.dictValue === value)
    return item ? item.dictLabel : String(value)
  }

  function getDictClass(dictType, value) {
    const list = dictData.value[dictType] || []
    const item = list.find(d => d.dictValue === String(value) || d.dictValue === value)
    return item?.cssClass || ''
  }

  function clearDict() {
    dictData.value = {}
  }

  return { dictData, loadDict, getDictLabel, getDictClass, clearDict }
})