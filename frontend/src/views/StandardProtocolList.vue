<template>
  <div class="standard-protocol-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标准协议管理</span>
          <div>
            <el-button type="success" @click="openImportDialog">从文档导入</el-button>
            <el-button type="primary" @click="openProtocolDialog">新建协议</el-button>
          </div>
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

    <!-- 文档导入对话框 -->
    <el-dialog 
      v-model="importDialogVisible" 
      title="从文档导入协议" 
      width="800px"
      @close="resetImportDialog"
    >
      <el-tabs v-model="importTab">
        <el-tab-pane label="上传文件" name="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".txt,.md,.json,.xml,.doc,.docx"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 txt、md、json、xml、doc、docx 格式文件
              </div>
            </template>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="粘贴文本" name="text">
          <el-input
            v-model="importText"
            type="textarea"
            :rows="15"
            placeholder="请粘贴包含报文结构的文档内容..."
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">开始导入</el-button>
      </template>
    </el-dialog>

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
import { UploadFilled } from '@element-plus/icons-vue'
import { standardProtocolApi } from '../api'

const protocolList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentProtocolId = ref(null)
const saving = ref(false)
const formRef = ref(null)

// 文档导入相关
const importDialogVisible = ref(false)
const importTab = ref('file')
const importText = ref('')
const importing = ref(false)
const uploadRef = ref(null)
const importFile = ref(null)

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

// 打开导入对话框
const openImportDialog = () => {
  importDialogVisible.value = true
  resetImportDialog()
}

// 重置导入对话框
const resetImportDialog = () => {
  importText.value = ''
  importFile.value = null
  importTab.value = 'file'
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  importFile.value = file.raw
}

// 执行导入
const handleImport = async () => {
  if (importTab.value === 'file' && !importFile.value) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  if (importTab.value === 'text' && (!importText.value || !importText.value.trim())) {
    ElMessage.warning('请输入文档内容')
    return
  }
  
  importing.value = true
  try {
    const response = await standardProtocolApi.importFromDocument(
      importTab.value === 'text' ? importText.value : null,
      importTab.value === 'file' ? importFile.value : null
    )
    
    if (response.data.success) {
      const count = response.data.count || 1
      const protocols = Array.isArray(response.data.data) ? response.data.data : [response.data.data]
      
      ElMessage.success(`成功导入 ${count} 条协议！`)
      importDialogVisible.value = false
      loadProtocols()
      
      // 显示导入结果
      if (protocols.length === 1) {
        // 单个协议，显示详细信息
        const protocol = protocols[0]
        ElMessageBox.alert(
          `协议名称：${protocol.name}\n协议编码：${protocol.code}\n协议类型：${protocol.protocolType}`,
          '导入成功',
          {
            confirmButtonText: '确定',
            type: 'success'
          }
        )
      } else {
        // 多个协议，显示摘要
        const protocolNames = protocols.map(p => p.name).join('、')
        ElMessageBox.alert(
          `成功导入 ${count} 条协议：\n${protocolNames}`,
          '导入成功',
          {
            confirmButtonText: '确定',
            type: 'success'
          }
        )
      }
    } else {
      ElMessage.error('导入失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.response?.data?.errorMessage || error.message))
  } finally {
    importing.value = false
  }
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

