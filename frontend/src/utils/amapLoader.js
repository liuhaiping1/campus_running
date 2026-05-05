import AMapLoader from '@amap/amap-jsapi-loader'

let loadPromise = null

/**
 * 加载高德地图 JSAPI v2.0（单例，重复调用返回同一 Promise）
 * @returns {Promise<AMap>}
 */
export function loadAmap() {
  if (loadPromise) return loadPromise

  window._AMapSecurityConfig = {
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_JS_CODE
  }

  loadPromise = AMapLoader.load({
    key: import.meta.env.VITE_AMAP_JS_KEY,
    version: '2.0',
    plugins: [
      'AMap.AutoComplete',
      'AMap.PlaceSearch',
      'AMap.Geocoder'
    ]
  }).catch(err => {
    loadPromise = null
    throw err
  })

  return loadPromise
}
