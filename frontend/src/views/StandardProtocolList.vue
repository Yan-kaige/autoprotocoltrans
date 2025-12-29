<template>
  <div class="standard-protocol-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标准协议管理</span>
          <el-button type="primary" @click="openProtocolDialog">新建协议</el-button>
        </div>
      </template>

      <el-table :data="protocolList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="协议名称" />
        <el-table-column prop="code" label="协议编码" />
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="protocolType" label="协议类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.protocolType === 'JSON' ? 'success' : 'warning'">
              {{ row.protocolType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="toggleEnabled(row.id)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editProtocol(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteProtocol(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 协议编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentProtocolId ? '编辑协议' : '新建协议'" 
      width="900px"
      @close="resetDialog"
    >
      <el-form :model="protocolForm" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="协议名称" prop="name">
          <el-input v-model="protocolForm.name" placeholder="请输入协议名称" />
        </el-form-item>
        <el-form-item label="协议编码" prop="code">
          <el-input v-model="protocolForm.code" placeholder="请输入协议编码（唯一）" />
        </el-form-item>
        <el-form-item label="协议分类">
          <el-input v-model="protocolForm.category" placeholder="如：用户信息、订单信息、支付信息等" />
        </el-form-item>
        <el-form-item label="协议类型" prop="protocolType">
          <el-select v-model="protocolForm.protocolType" placeholder="请选择协议类型">
            <el-option label="JSON" value="JSON" />
            <el-option label="XML" value="XML" />
          </el-select>
        </el-form-item>
        <el-form-item label="协议描述">
          <el-input 
            v-model="protocolForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入协议描述（可选）"
          />
        </el-form-item>
        <el-form-item label="数据格式" prop="dataFormat">
          <el-input 
            v-model="protocolForm.dataFormat" 
            type="textarea" 
            :rows="12"
            placeholder="请输入数据格式模板（JSON或XML）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProtocol" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { standardProtocolApi } from '../api'

const protocolList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentProtocolId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const protocolForm = ref({
  name: '',
  code: '',
  category: '',
  protocolType: 'JSON',
  description: '',
  dataFormat: ''
})

const rules = {
  name: [{ required: true, message: '请输入协议名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入协议编码', trigger: 'blur' }],
  protocolType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  dataFormat: [{ required: true, message: '请输入数据格式', trigger: 'blur' }]
}

onMounted(() => {
  loadProtocols()
})

const loadProtocols = async () => {
  loading.value = true
  try {
    const response = await standardProtocolApi.getAllProtocols()
    if (response.data.success) {
      protocolList.value = response.data.data || []
    } else {
      ElMessage.error('加载协议列表失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    ElMessage.error('加载协议列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const openProtocolDialog = () => {
  currentProtocolId.value = null
  resetDialog()
  dialogVisible.value = true
}

const editProtocol = async (id) => {
  try {
    const response = await standardProtocolApi.getProtocolById(id)
    if (response.data.success) {
      const protocol = response.data.data
      currentProtocolId.value = id
      protocolForm.value = {
        name: protocol.name || '',
        code: protocol.code || '',
        category: protocol.category || '',
        protocolType: protocol.protocolType || 'JSON',
        description: protocol.description || '',
        dataFormat: protocol.dataFormat || ''
      }
      dialogVisible.value = true
    } else {
      ElMessage.error('加载协议失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    ElMessage.error('加载协议失败: ' + error.message)
  }
}

const saveProtocol = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      // 验证数据格式
      if (protocolForm.value.protocolType === 'JSON') {
        try {
          JSON.parse(protocolForm.value.dataFormat)
        } catch (e) {
          ElMessage.error('JSON格式错误: ' + e.message)
          saving.value = false
          return
        }
      }
      
      const response = await standardProtocolApi.saveProtocol(
        currentProtocolId.value,
        protocolForm.value.name,
        protocolForm.value.code,
        protocolForm.value.description,
        protocolForm.value.protocolType,
        protocolForm.value.dataFormat,
        protocolForm.value.category
      )
      
      if (response.data.success) {
        ElMessage.success(currentProtocolId.value ? '协议更新成功' : '协议保存成功')
        dialogVisible.value = false
        loadProtocols()
      } else {
        ElMessage.error('保存失败: ' + response.data.errorMessage)
      }
    } catch (error) {
      ElMessage.error('保存失败: ' + error.message)
    } finally {
      saving.value = false
    }
  })
}

const deleteProtocol = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该协议吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await standardProtocolApi.deleteProtocol(id)
    if (response.data.success) {
      ElMessage.success('删除成功')
      loadProtocols()
    } else {
      ElMessage.error('删除失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const toggleEnabled = async (id) => {
  try {
    const response = await standardProtocolApi.toggleEnabled(id)
    if (response.data.success) {
      ElMessage.success('状态更新成功')
      loadProtocols()
    } else {
      ElMessage.error('状态更新失败: ' + response.data.errorMessage)
      loadProtocols() // 重新加载以恢复状态
    }
  } catch (error) {
    ElMessage.error('状态更新失败: ' + error.message)
    loadProtocols() // 重新加载以恢复状态
  }
}

const resetDialog = () => {
  protocolForm.value = {
    name: '',
    code: '',
    category: '',
    protocolType: 'JSON',
    description: '',
    dataFormat: ''
  }
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.standard-protocol-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

