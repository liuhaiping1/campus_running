import { ref, computed } from 'vue'

// 模块级单例：所有组件共享同一个 resize 监听器
const windowWidth = ref(window.innerWidth)
let listenerCount = 0

function onResize() {
  windowWidth.value = window.innerWidth
}

export function useResponsive(breakpoint = 768) {
  const isMobile = computed(() => windowWidth.value <= breakpoint)

  // 第一个使用者注册监听器，后续复用
  if (listenerCount === 0) {
    window.addEventListener('resize', onResize)
  }
  listenerCount++

  // 返回 cleanup 函数供调用方按需清理（通常不需要，SPA 生命周期有限）
  function cleanup() {
    listenerCount--
    if (listenerCount <= 0) {
      window.removeEventListener('resize', onResize)
      listenerCount = 0
    }
  }

  return { windowWidth, isMobile, cleanup }
}
