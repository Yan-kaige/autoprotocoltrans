<template>
  <div class="config-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>配置管理</span>
          <el-button type="primary" @click="goToEditor">新建配置</el-button>
        </div>
      </template>

      <el-table :data="configList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="配置名称" />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editConfig(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteConfig(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { configApi } from '../api'

const router = useRouter()
const configList = ref([])
const loading = ref(false)

onMounted(() => {
  loadConfigList()
})

const loadConfigList = async () => {
  loading.value = true
  try {
    const res = await configApi.getAllConfigs()
    if (res.data.success) {
      configList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.errorMessage || '加载配置列表失败')
    }
  } catch (e) {
    console.error('加载配置列表失败:', e)
    ElMessage.error('加载配置列表失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  // 处理ISO 8601格式的日期时间字符串
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

const goToEditor = () => {
  router.push({ path: '/canvas' })
}

const editConfig = (configId) => {
  router.push({ path: '/canvas', query: { configId } })
}

const deleteConfig = async (configId) => {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await configApi.deleteConfig(configId)
    if (res.data.success) {
      ElMessage.success('删除成功')
      loadConfigList()
    } else {
      ElMessage.error(res.data.errorMessage || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除配置失败:', e)
      ElMessage.error('删除失败: ' + (e.response?.data?.errorMessage || e.message || '未知错误'))
    }
  }
}
</script>

<style scoped>
.config-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

