<template>
  <div class="rule-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>转换规则列表</span>
          <el-button type="primary" @click="createNewRule">
            <el-icon><Plus /></el-icon>
            新建规则
          </el-button>
        </div>
      </template>
      
      <el-table :data="rules" stripe style="width: 100%">
        <el-table-column prop="name" label="规则名称" width="200" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="源协议 -> 目标协议" width="200">
          <template #default="scope">
            {{ getProtocolName(scope.row.sourceType) }} -> {{ getProtocolName(scope.row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">
              {{ scope.row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button link type="primary" @click="editRule(scope.row.id)">
              编辑
            </el-button>
            <el-button link type="danger" @click="deleteRule(scope.row.id)">
              删除
            </el-button>
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
import { Plus } from '@element-plus/icons-vue'
import { ruleApi } from '../api'

const router = useRouter()
const rules = ref([])

// 获取协议名称（支持字符串和对象两种格式）
const getProtocolName = (protocol) => {
  if (!protocol) return '-'
  if (typeof protocol === 'string') {
    return protocol
  }
  return protocol.name || protocol.toString()
}

onMounted(() => {
  loadRules()
})

const loadRules = async () => {
  try {
    const response = await ruleApi.getAllRules()
    rules.value = response.data
  } catch (error) {
    ElMessage.error('加载规则列表失败: ' + error.message)
  }
}

const createNewRule = () => {
  router.push('/rule')
}

const editRule = (id) => {
  router.push(`/rule/${id}`)
}

const deleteRule = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个规则吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await ruleApi.deleteRule(id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}
</script>

<style scoped>
.rule-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

