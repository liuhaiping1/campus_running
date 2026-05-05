<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listAddress, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import ConfirmActionButton from '@/components/ConfirmActionButton.vue'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const addresses = ref([])
const { isMobile } = useResponsive()
const dialogVisible = ref(false)
const dialogTitle = ref('新增地址')
const isEdit = ref(false)
const editId = ref(null)

const formRef = ref(null)
const form = reactive({
  contactName: '',
  contactPhone: '',
  campusName: '',
  buildingName: '',
  detailAddress: '',
  isDefault: 0
})

// 表单校验规则，与后端 AddressSaveRequest 一致
const rules = {
  contactName: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
    { max: 32, message: '联系人不超过32位', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  campusName: [
    { required: true, message: '请输入校区', trigger: 'blur' },
    { max: 64, message: '校区不超过64位', trigger: 'blur' }
  ],
  buildingName: [
    { required: true, message: '请输入楼栋', trigger: 'blur' },
    { max: 64, message: '楼栋不超过64位', trigger: 'blur' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 255, message: '详细地址不超过255位', trigger: 'blur' }
  ]
}

async function fetchAddresses() {
  loading.value = true
  try {
    addresses.value = await listAddress()
  } catch {
    addresses.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = '新增地址'
  isEdit.value = false
  editId.value = null
  Object.assign(form, {
    contactName: '', contactPhone: '', campusName: '',
    buildingName: '', detailAddress: '', isDefault: 0
  })
  dialogVisible.value = true
  // 清除上次校验状态
  formRef.value?.resetFields()
}

function openEdit(address) {
  dialogTitle.value = '编辑地址'
  isEdit.value = true
  editId.value = address.id
  Object.assign(form, {
    contactName: address.contactName,
    contactPhone: address.contactPhone,
    campusName: address.campusName,
    buildingName: address.buildingName,
    detailAddress: address.detailAddress,
    isDefault: address.isDefault || 0
  })
  dialogVisible.value = true
  formRef.value?.resetFields()
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (isEdit.value) {
    await updateAddress(editId.value, form)
    ElMessage.success('修改成功')
  } else {
    await createAddress(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchAddresses()
}

async function handleDelete(id) {
  await deleteAddress(id)
  ElMessage.success('删除成功')
  fetchAddresses()
}

async function handleSetDefault(id) {
  await setDefaultAddress(id)
  ElMessage.success('默认地址设置成功')
  fetchAddresses()
}

onMounted(fetchAddresses)
</script>

<template>
  <PageContainer title="地址管理" subtitle="管理常用收货/取件地址">
    <template #extra>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增地址</el-button>
    </template>

    <div v-loading="loading" class="address-page">
      <EmptyState
        v-if="!loading && addresses.length === 0"
        description="暂无地址，点击右上角新增"
        action-text="新增地址"
        @action="openCreate"
      />

      <div v-else class="address-grid responsive-grid">
        <div
          v-for="address in addresses"
          :key="address.id"
          class="address-card"
          :class="{ 'is-default': address.isDefault === 1 }"
        >
          <div class="card-top">
            <div class="card-header">
              <span class="contact-name">{{ address.contactName }}</span>
              <span class="contact-phone">{{ address.contactPhone }}</span>
              <el-tag v-if="address.isDefault === 1" type="success" size="small">默认</el-tag>
            </div>
            <p class="address-text">{{ address.campusName }} {{ address.buildingName }} {{ address.detailAddress }}</p>
          </div>
          <div class="card-actions">
            <el-button text type="primary" size="small" @click="openEdit(address)">编辑</el-button>
            <el-button
              v-if="address.isDefault !== 1"
              text
              type="success"
              size="small"
              :loading="false"
              @click="handleSetDefault(address.id)"
            >
              设为默认
            </el-button>
            <ConfirmActionButton
              text="删除"
              confirm-text="确定删除该地址吗？"
              size="small"
              :action="() => handleDelete(address.id)"
              success-text="删除成功"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="isMobile ? '95%' : '520px'"
      :close-on-click-modal="false"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="right">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="联系人姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" type="tel" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="校区" prop="campusName">
          <el-input v-model="form.campusName" placeholder="如：主校区" maxlength="64" />
        </el-form-item>
        <el-form-item label="楼栋" prop="buildingName">
          <el-input v-model="form.buildingName" placeholder="如：一号宿舍楼" maxlength="64" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" placeholder="如：101室" maxlength="255" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.address-page {
  min-height: 200px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.address-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.3s;
}
.address-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.address-card.is-default {
  border-color: #67c23a;
}

.card-top {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.contact-phone {
  font-size: 13px;
  color: #909399;
}

.address-text {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.6;
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

@media (max-width: 768px) {
  .address-grid {
    grid-template-columns: 1fr;
  }
  .address-card {
    padding: 16px;
  }
}
</style>
