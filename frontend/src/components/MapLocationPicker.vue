<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { Location, Close, Loading } from '@element-plus/icons-vue'
import { loadAmap } from '@/utils/amapLoader'
import { listAddress } from '@/api/address'
import { getCampusLocations } from '@/api/map'
import { addressToLocation } from '@/utils/address'
import { useResponsive } from '@/composables/useResponsive'

const props = defineProps({
  modelValue: { type: Object, default: null },
  placeholder: { type: String, default: '点击选择位置' },
  addresses: { type: Array, default: null }
})

const emit = defineEmits(['update:modelValue'])
const { isMobile } = useResponsive()

const TAB_ADDRESS = 'address'
const TAB_CAMPUS = 'campus'

const dialogVisible = ref(false)
const mapInstance = ref(null)
const markerInstance = ref(null)
const geocoderInstance = ref(null)
const placeSearchInstance = ref(null)
const autoCompleteInstance = ref(null)
let AMapRef = null

const mapLoading = ref(true)
const mapError = ref('')
const activeTab = ref(TAB_ADDRESS)
const addressList = ref([])
const campusList = ref([])
function openDialog() {
  dialogVisible.value = true
  fetchLists()
}

async function initMap() {
  if (mapInstance.value) return

  try {
    mapLoading.value = true
    mapError.value = ''
    AMapRef = await loadAmap()

    if (!dialogVisible.value) return

    mapInstance.value = new AMapRef.Map('map-container', {
      zoom: 15,
      viewMode: '2D'
    })

    geocoderInstance.value = new AMapRef.Geocoder()
    placeSearchInstance.value = new AMapRef.PlaceSearch({})

    autoCompleteInstance.value = new AMapRef.AutoComplete({
      input: 'map-search-input'
    })
    autoCompleteInstance.value.on('select', handleSearchSelect)

    mapInstance.value.on('click', handleMapClick)

    if (props.modelValue?.lng && props.modelValue?.lat) {
      placeMarker(props.modelValue.lng, props.modelValue.lat)
      mapInstance.value.setCenter([props.modelValue.lng, props.modelValue.lat])
    }

    mapLoading.value = false
  } catch {
    mapLoading.value = false
    mapError.value = '地图加载失败，请从左侧列表选择地址'
  }
}

function placeMarker(lng, lat) {
  if (!AMapRef || !mapInstance.value) return

  if (markerInstance.value) {
    markerInstance.value.setPosition([lng, lat])
  } else {
    markerInstance.value = new AMapRef.Marker({
      position: [lng, lat],
      map: mapInstance.value
    })
  }
  mapInstance.value.setCenter([lng, lat])
}

function emitSelection(name, lng, lat) {
  emit('update:modelValue', { name, lng: Number(lng), lat: Number(lat) })
  dialogVisible.value = false
}

function handleClear() {
  emit('update:modelValue', null)
}

function handleMapClick(e) {
  const { lng, lat } = e.lnglat
  placeMarker(lng, lat)

  geocoderInstance.value.getAddress([lng, lat], (status, result) => {
    const name = status === 'complete'
      ? result.regeocode.formattedAddress
      : `${lng.toFixed(6)}, ${lat.toFixed(6)}`
    emitSelection(name, lng, lat)
  })
}

function handleSearchSelect(e) {
  if (!e.poi?.name) return

  placeSearchInstance.value.search(e.poi.name, (status, result) => {
    if (status === 'complete' && result.poiList?.pois?.length) {
      const poi = result.poiList.pois[0]
      const { lng, lat } = poi.location
      placeMarker(lng, lat)
      emitSelection(poi.name, lng, lat)
    }
  })
}

function selectFromList(item, type) {
  const loc = addressToLocation(item)
  if (type === TAB_CAMPUS) {
    loc.name = item.detailAddress || item.locationName
  }
  placeMarker(loc.lng, loc.lat)
  emitSelection(loc.name, loc.lng, loc.lat)
}

async function fetchLists() {
  const addrPromise = props.addresses
    ? Promise.resolve(props.addresses)
    : listAddress().catch(() => [])
  const [addrs, campus] = await Promise.all([
    addrPromise,
    getCampusLocations().catch(() => [])
  ])
  addressList.value = addrs
  campusList.value = campus
}

function cleanupMap() {
  if (!mapInstance.value) return
  autoCompleteInstance.value?.off('select', handleSearchSelect)
  autoCompleteInstance.value = null
  mapInstance.value.off('click', handleMapClick)
  mapInstance.value.destroy()
  mapInstance.value = null
  markerInstance.value = null
  geocoderInstance.value = null
  placeSearchInstance.value = null
  AMapRef = null
}

watch(dialogVisible, (val) => {
  if (!val) cleanupMap()
})

onBeforeUnmount(cleanupMap)
</script>

<template>
  <el-input
    :model-value="modelValue?.name || ''"
    :placeholder="placeholder"
    readonly
    class="map-picker-trigger"
    @click="openDialog"
  >
    <template #prefix>
      <el-icon><Location /></el-icon>
    </template>
    <template #suffix v-if="modelValue">
      <el-icon class="clear-icon" @click.stop="handleClear"><Close /></el-icon>
    </template>
  </el-input>

  <el-dialog
    v-model="dialogVisible"
    title="选择位置"
    :width="isMobile ? '95%' : '750px'"
    :close-on-click-modal="false"
    destroy-on-close
    @opened="initMap"
  >
    <div class="picker-body">
      <div class="search-bar">
        <input
          id="map-search-input"
          type="text"
          placeholder="搜索地点..."
          class="search-input"
        />
      </div>

      <div class="picker-content">
        <div class="picker-sidebar">
          <el-tabs v-model="activeTab" class="picker-tabs">
            <el-tab-pane label="我的地址" :name="TAB_ADDRESS">
              <div class="list-wrap">
                <div
                  v-for="item in addressList"
                  :key="item.id"
                  class="list-item"
                  @click="selectFromList(item, TAB_ADDRESS)"
                >
                  <div class="item-name">{{ item.contactName }} - {{ item.buildingName }}</div>
                  <div class="item-detail">{{ item.campusName }} {{ item.buildingName }} {{ item.detailAddress }}</div>
                </div>
                <el-empty
                  v-if="!addressList.length"
                  description="暂无保存的地址"
                  :image-size="48"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane label="校园地点" :name="TAB_CAMPUS">
              <div class="list-wrap">
                <div
                  v-for="item in campusList"
                  :key="item.id"
                  class="list-item"
                  @click="selectFromList(item, TAB_CAMPUS)"
                >
                  <div class="item-name">{{ item.locationName }}</div>
                  <div class="item-detail">{{ item.campusName }} {{ item.buildingName }}</div>
                </div>
                <el-empty
                  v-if="!campusList.length"
                  description="暂无校园地点"
                  :image-size="48"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="picker-map">
          <div id="map-container" class="map-container"></div>
          <div v-if="mapLoading" class="map-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>地图加载中...</span>
          </div>
          <div v-if="mapError" class="map-error">{{ mapError }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-hint">点击地图选择位置，或从左侧列表选择</span>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.map-picker-trigger {
  cursor: pointer;
}

.map-picker-trigger :deep(.el-input__inner) {
  cursor: pointer;
}

.clear-icon {
  cursor: pointer;
  color: var(--text-secondary);
}

.clear-icon:hover {
  color: var(--text-primary);
}

.picker-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-bar {
  position: relative;
}

.search-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-button);
  font-size: var(--font-body);
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: var(--color-primary);
}

.picker-content {
  display: flex;
  gap: 12px;
  min-height: 400px;
}

.picker-sidebar {
  width: 240px;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  overflow: hidden;
}

.picker-tabs {
  height: 100%;
}

.picker-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.picker-tabs :deep(.el-tabs__content) {
  padding: 0;
  height: calc(100% - 40px);
  overflow: hidden;
}

.picker-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.list-wrap {
  height: 356px;
  overflow-y: auto;
}

.list-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color);
  transition: background-color 0.15s;
}

.list-item:hover {
  background-color: var(--bg-page);
}

.list-item:last-child {
  border-bottom: none;
}

.item-name {
  font-size: var(--font-body);
  color: var(--text-primary);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-detail {
  font-size: var(--font-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-map {
  flex: 1;
  position: relative;
  border-radius: var(--radius-card);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.map-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.map-loading,
.map-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--font-body);
  background: rgba(255, 255, 255, 0.9);
  padding: 12px 20px;
  border-radius: var(--radius-card);
}

.map-error {
  color: var(--color-danger);
}

.dialog-hint {
  float: left;
  font-size: var(--font-sm);
  color: var(--text-secondary);
  line-height: 32px;
}

@media (max-width: 768px) {
  .picker-content {
    flex-direction: column;
  }

  .picker-sidebar {
    width: 100%;
  }

  .list-wrap {
    height: 200px;
  }

  .map-container {
    min-height: 300px;
  }
}
</style>
