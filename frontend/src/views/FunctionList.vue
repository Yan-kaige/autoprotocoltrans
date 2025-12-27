<template>
  <div class="function-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>函数管理</span>
          <el-button type="primary" @click="openFunctionDialog">新建函数</el-button>
        </div>
      </template>

      <el-table :data="functionList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="函数名称" />
        <el-table-column prop="code" label="函数编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
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
            <el-button type="primary" size="small" @click="editFunction(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteFunction(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 函数编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentFunctionId ? '编辑函数' : '新建函数'" 
      width="800px"
      @close="resetDialog"
    >
      <el-form :model="functionForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="函数名称" prop="name">
          <el-input v-model="functionForm.name" placeholder="请输入函数名称" />
        </el-form-item>
        <el-form-item label="函数编码" prop="code">
          <el-input v-model="functionForm.code" placeholder="请输入函数编码（唯一，用于调用）" />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            编码将作为函数调用时的名称，例如：myFunction
          </div>
        </el-form-item>
        <el-form-item label="函数描述">
          <el-input 
            v-model="functionForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入函数描述（可选）"
          />
        </el-form-item>
        <el-form-item label="Groovy脚本" prop="script">
          <el-input 
            v-model="functionForm.script" 
            type="textarea" 
            :rows="8"
            placeholder="请输入Groovy脚本代码。脚本应返回转换结果，输入值通过input变量访问。&#10;示例：return input.toString().toUpperCase()"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            脚本说明：输入值通过 <code>input</code> 变量访问，需要返回转换后的结果
          </div>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="functionForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFunction" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { functionApi } from '../api'
const functionList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentFunctionId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const functionForm = ref({
  name: '',
  code: '',
  description: '',
  script: '',
  enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入函数名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入函数编码', trigger: 'blur' }],
  script: [{ required: true, message: '请输入Groovy脚本', trigger: 'blur' }]
}

onMounted(() => {
  loadFunctionList()
})

const loadFunctionList = async () => {
  loading.value = true
  try {
    const res = await functionApi.getAllFunctions()
    if (res.data.success) {
      functionList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.errorMessage || '加载函数列表失败')
    }
  } catch (e) {
    console.error('加载函数列表失败:', e)
    ElMessage.error('加载函数列表失败: ' + (e.message || '未知错误'))
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

const openFunctionDialog = () => {
  currentFunctionId.value = null
  resetDialog()
  dialogVisible.value = true
}

const editFunction = async (id) => {
  try {
    const res = await functionApi.getFunction(id)
    if (res.data.success) {
      const data = res.data.data
      currentFunctionId.value = id
      functionForm.value = {
        name: data.name,
        code: data.code,
        description: data.description || '',
        script: data.script || '',
        enabled: data.enabled !== undefined ? data.enabled : true
      }
      dialogVisible.value = true
    } else {
      ElMessage.error(res.data.errorMessage || '加载函数失败')
    }
  } catch (e) {
    console.error('加载函数失败:', e)
    ElMessage.error('加载函数失败: ' + (e.message || '未知错误'))
  }
}

const resetDialog = () => {
  functionForm.value = {
    name: '',
    code: '',
    description: '',
    script: '',
    enabled: true
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const saveFunction = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    saving.value = true
    const res = await functionApi.saveFunction(
      currentFunctionId.value,
      functionForm.value.name.trim(),
      functionForm.value.code.trim(),
      functionForm.value.description?.trim() || '',
      functionForm.value.script.trim(),
      functionForm.value.enabled
    )
    
    if (res.data.success) {
      ElMessage.success(currentFunctionId.value ? '函数更新成功' : '函数保存成功')
      dialogVisible.value = false
      loadFunctionList()
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

const deleteFunction = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该函数吗？删除后使用该函数的映射配置将无法正常工作。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await functionApi.deleteFunction(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      loadFunctionList()
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
.function-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

code {
  background-color: #f4f4f5;
  color: #e6a23c;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}
</style>


