<template>
  <div class="transaction-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <span>{{ bankCategory }} - 交易类型列表</span>
          <el-button type="primary" @click="goToEditor">新建配置</el-button>
        </div>
      </template>

      <el-table :data="transactionList" v-loading="loading" style="width: 100%">
        <el-table-column prop="requestType" label="交易类型" />
        <el-table-column label="请求配置" width="150" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.requestConfig" type="success">已配置</el-tag>
            <el-tag v-else type="info">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="响应配置" width="150" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.responseConfig" type="success">已配置</el-tag>
            <el-tag v-else type="info">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="editConfig(row.requestType, 'REQUEST')"
              :disabled="!row.requestConfig"
            >
              编辑请求
            </el-button>
            <el-button 
              type="success" 
              size="small" 
              @click="editConfig(row.requestType, 'RESPONSE')"
              :disabled="!row.responseConfig"
            >
              编辑响应
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              plain
              @click="editConfig(row.requestType, null)"
            >
              新增/编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { configApi } from '../api'

const router = useRouter()
const route = useRoute()
const bankCategory = ref('')
const transactionList = ref([])
const loading = ref(false)

onMounted(() => {
  bankCategory.value = route.query.bankCategory || ''
  if (bankCategory.value) {
    loadTransactionList()
  }
})

const loadTransactionList = async () => {
  loading.value = true
  try {
    // 获取所有交易类型
    const typesRes = await configApi.getTransactionTypes()
    const transactionTypes = typesRes.data.success ? typesRes.data.data.map(t => t.value) : []
    
    // 获取该银行的所有配置
    const configsRes = await configApi.getAllConfigs()
    const allConfigs = configsRes.data.success ? configsRes.data.data : []
    
    // 按银行类别过滤
    const bankConfigs = allConfigs.filter(c => c.bankCategory === bankCategory.value)
    
    // 构建交易类型列表，显示每个交易类型的请求/响应配置状态
    const transactionMap = new Map()
    
    // 初始化所有交易类型
    transactionTypes.forEach(type => {
      transactionMap.set(type, {
        requestType: type,
        requestConfig: null,
        responseConfig: null
      })
    })
    
    // 填充配置信息
    bankConfigs.forEach(config => {
      try {
        const configContent = JSON.parse(config.configContent || '{}')
        const configType = configContent.configType || 'REQUEST'
        const requestType = config.requestType
        
        if (requestType && transactionMap.has(requestType)) {
          const item = transactionMap.get(requestType)
          if (configType === 'REQUEST') {
            item.requestConfig = config
          } else if (configType === 'RESPONSE') {
            item.responseConfig = config
          }
        }
      } catch (e) {
        console.error('解析配置失败:', e)
      }
    })
    
    transactionList.value = Array.from(transactionMap.values())
  } catch (e) {
    console.error('加载交易类型列表失败:', e)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push({ path: '/config' })
}

const goToEditor = () => {
  router.push({ 
    path: '/canvas',
    query: { 
      bankCategory: bankCategory.value,
      mode: 'new'
    }
  })
}

const editConfig = (requestType, configType) => {
  if (configType) {
    // 编辑指定类型的配置
    router.push({
      path: '/canvas',
      query: {
        bankCategory: bankCategory.value,
        requestType: requestType,
        configType: configType
      }
    })
  } else {
    // 新增或编辑（让用户在编辑页面选择）
    router.push({
      path: '/canvas',
      query: {
        bankCategory: bankCategory.value,
        requestType: requestType
      }
    })
  }
}
</script>

<style scoped>
.transaction-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
</style>

