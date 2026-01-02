<template>
  <div class="bank-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>银行信息管理</span>
          <el-button type="primary" @click="openBankDialog">新建银行</el-button>
        </div>
      </template>

      <el-table :data="bankList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="银行名称" />
        <el-table-column prop="code" label="银行编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editBank(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteBank(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 银行编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentBankId ? '编辑银行' : '新建银行'" 
      width="600px"
      @close="resetDialog"
    >
      <el-form :model="bankForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="银行名称" prop="name">
          <el-input v-model="bankForm.name" placeholder="请输入银行名称，如：工商银行" />
        </el-form-item>
        <el-form-item label="银行编码" prop="code">
          <el-input v-model="bankForm.code" placeholder="请输入银行编码（唯一），如：ICBC" />
        </el-form-item>
        <el-form-item label="银行描述">
          <el-input 
            v-model="bankForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入银行描述（可选）"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="bankForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBank" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bankApi } from '../api'

const bankList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentBankId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const bankForm = ref({
  name: '',
  code: '',
  description: '',
  enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入银行编码', trigger: 'blur' }]
}

onMounted(() => {
  loadBankList()
})

const loadBankList = async () => {
  loading.value = true
  try {
    const res = await bankApi.getAllBanks()
    if (res.data.success) {
      bankList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.errorMessage || '加载银行列表失败')
    }
  } catch (e) {
    console.error('加载银行列表失败:', e)
    ElMessage.error('加载银行列表失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const openBankDialog = () => {
  currentBankId.value = null
  resetDialog()
  dialogVisible.value = true
}

const editBank = async (id) => {
  try {
    const res = await bankApi.getBankById(id)
    if (res.data.success) {
      const data = res.data.data
      currentBankId.value = id
      bankForm.value = {
        name: data.name || '',
        code: data.code || '',
        description: data.description || '',
        enabled: data.enabled !== undefined ? data.enabled : true
      }
      dialogVisible.value = true
    } else {
      ElMessage.error(res.data.errorMessage || '加载银行信息失败')
    }
  } catch (e) {
    console.error('加载银行信息失败:', e)
    ElMessage.error('加载银行信息失败: ' + (e.message || '未知错误'))
  }
}

const resetDialog = () => {
  bankForm.value = {
    name: '',
    code: '',
    description: '',
    enabled: true
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const saveBank = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    saving.value = true
    const res = await bankApi.saveBank(
      currentBankId.value,
      bankForm.value.name.trim(),
      bankForm.value.code.trim(),
      bankForm.value.description?.trim() || '',
      bankForm.value.enabled
    )
    
    if (res.data.success) {
      // ElMessage.success(currentBankId.value ? '银行信息更新成功' : '银行信息保存成功')
      dialogVisible.value = false
      loadBankList()
    } else {
      ElMessage.error(res.data.errorMessage || '保存失败')
    }
  } catch (e) {
    const errorMsg = e.response?.data?.errorMessage || e.message || '保存失败'
    ElMessage.error(errorMsg)
  } finally {
    saving.value = false
  }
}

const deleteBank = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该银行信息吗？删除后使用该银行的配置将无法正常工作。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await bankApi.deleteBank(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      loadBankList()
    } else {
      ElMessage.error(res.data.errorMessage || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      const errorMsg = e.response?.data?.errorMessage || e.message || '删除失败'
      ElMessage.error(errorMsg)
    }
  }
}
</script>

<style scoped>
.bank-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

