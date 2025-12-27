<template>
  <div class="dictionary-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字典管理</span>
          <el-button type="primary" @click="openDictionaryDialog">新建字典</el-button>
        </div>
      </template>

      <el-table :data="dictionaryList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="字典名称" />
        <el-table-column prop="code" label="字典编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
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
            <el-button type="primary" size="small" @click="editDictionary(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteDictionary(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentDictionaryId ? '编辑字典' : '新建字典'" 
      width="800px"
      @close="resetDialog"
    >
      <el-form :model="dictionaryForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="字典名称" prop="name">
          <el-input v-model="dictionaryForm.name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典编码" prop="code">
          <el-input v-model="dictionaryForm.code" placeholder="请输入字典编码（唯一）" />
        </el-form-item>
        <el-form-item label="字典描述">
          <el-input 
            v-model="dictionaryForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入字典描述（可选）"
          />
        </el-form-item>
        <el-form-item label="字典项">
          <div class="dict-items-container">
            <div v-for="(item, index) in dictionaryForm.items" :key="index" class="dict-item-row">
              <el-input v-model="item.key" placeholder="键（Key）" style="width: 40%" />
              <span style="margin: 0 10px">-></span>
              <el-input v-model="item.value" placeholder="值（Value）" style="width: 40%" />
              <el-button link type="danger" @click="removeDictItem(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="addDictItem" style="margin-top: 10px">添加字典项</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDictionary" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { dictionaryApi } from '../api'

const dictionaryList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentDictionaryId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const dictionaryForm = ref({
  name: '',
  code: '',
  description: '',
  items: [{ key: '', value: '' }]
})

const rules = {
  name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入字典编码', trigger: 'blur' }]
}

onMounted(() => {
  loadDictionaryList()
})

const loadDictionaryList = async () => {
  loading.value = true
  try {
    const res = await dictionaryApi.getAllDictionaries()
    if (res.data.success) {
      dictionaryList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.errorMessage || '加载字典列表失败')
    }
  } catch (e) {
    console.error('加载字典列表失败:', e)
    ElMessage.error('加载字典列表失败: ' + (e.message || '未知错误'))
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

const openDictionaryDialog = () => {
  currentDictionaryId.value = null
  resetDialog()
  dialogVisible.value = true
}

const editDictionary = async (id) => {
  try {
    const res = await dictionaryApi.getDictionary(id)
    if (res.data.success) {
      const data = res.data.data
      currentDictionaryId.value = id
      dictionaryForm.value = {
        name: data.dictionary.name,
        code: data.dictionary.code,
        description: data.dictionary.description || '',
        items: data.items && data.items.length > 0 
          ? data.items.map(item => ({ key: item.key, value: item.value }))
          : [{ key: '', value: '' }]
      }
      dialogVisible.value = true
    } else {
      ElMessage.error(res.data.errorMessage || '加载字典失败')
    }
  } catch (e) {
    console.error('加载字典失败:', e)
    ElMessage.error('加载字典失败: ' + (e.message || '未知错误'))
  }
}

const addDictItem = () => {
  dictionaryForm.value.items.push({ key: '', value: '' })
}

const removeDictItem = (index) => {
  dictionaryForm.value.items.splice(index, 1)
}

const resetDialog = () => {
  dictionaryForm.value = {
    name: '',
    code: '',
    description: '',
    items: [{ key: '', value: '' }]
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const saveDictionary = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    // 验证字典项
    const validItems = dictionaryForm.value.items.filter(item => item.key && item.value)
    if (validItems.length === 0) {
      ElMessage.warning('请至少添加一个有效的字典项')
      return
    }
    
    // 检查键是否重复
    const keys = validItems.map(item => item.key)
    if (new Set(keys).size !== keys.length) {
      ElMessage.warning('字典项的键不能重复')
      return
    }
    
    saving.value = true
    const res = await dictionaryApi.saveDictionary(
      currentDictionaryId.value,
      dictionaryForm.value.name.trim(),
      dictionaryForm.value.code.trim(),
      dictionaryForm.value.description?.trim() || '',
      validItems
    )
    
    if (res.data.success) {
      ElMessage.success(currentDictionaryId.value ? '字典更新成功' : '字典保存成功')
      dialogVisible.value = false
      loadDictionaryList()
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

const deleteDictionary = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该字典吗？删除后使用该字典的映射配置将无法正常工作。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await dictionaryApi.deleteDictionary(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      loadDictionaryList()
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
.dictionary-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dict-items-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.dict-item-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.dict-item-row:last-child {
  margin-bottom: 0;
}
</style>

