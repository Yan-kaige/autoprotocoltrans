<template>
  <div class="transaction-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <span>{{ bankName }} - 交易类型列表</span>
        </div>
      </template>

      <el-table :data="transactionList" v-loading="loading" style="width: 100%">
        <el-table-column prop="transactionName" label="交易类型" />
        <el-table-column label="版本" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.currentVersion || 'v1' }}</el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="editConfig(row.id, 'REQUEST')"
              :disabled="!row.requestConfig"
            >
              编辑请求
            </el-button>
            <el-button 
              type="success" 
              size="small" 
              @click="editConfig(row.id, 'RESPONSE')"
              :disabled="!row.responseConfig"
            >
              编辑响应
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              plain
              @click="editConfig(row.id, null)"
            >
              新增/编辑
            </el-button>
            <el-button 
              type="warning" 
              size="small" 
              @click="showVersionDialog(row)"
            >
              版本管理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 版本管理对话框 -->
      <el-dialog 
        v-model="versionDialogVisible" 
        title="版本管理" 
        width="600px"
      >
        <div v-if="currentTransaction">
          <p><strong>交易类型：</strong>{{ currentTransaction.transactionName }}</p>
          <p><strong>当前版本：</strong>{{ currentTransaction.currentVersion || 'v1' }}</p>
          <el-table :data="versionList" style="width: 100%; margin-top: 20px;">
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column label="请求配置" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.hasRequest" type="success">已配置</el-tag>
                <el-tag v-else type="info">未配置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="响应配置" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.hasResponse" type="success">已配置</el-tag>
                <el-tag v-else type="info">未配置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-button 
                  v-if="row.hasRequest"
                  type="primary" 
                  size="small" 
                  @click="viewVersion(row.version, 'REQUEST')"
                >
                  查看请求
                </el-button>
                <el-button 
                  v-if="row.hasResponse"
                  type="success" 
                  size="small" 
                  @click="viewVersion(row.version, 'RESPONSE')"
                >
                  查看响应
                </el-button>
                <el-button 
                  v-if="!row.hasRequest && !row.hasResponse"
                  type="info" 
                  size="small" 
                  disabled
                >
                  无配置
                </el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="switchVersion(row.version)"
                  :disabled="row.version === currentTransaction.currentVersion"
                >
                  切换
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { transactionTypeApi, configV2Api } from '../api'

const router = useRouter()
const route = useRoute()
const bankId = ref(null)
const bankName = ref('')
const transactionList = ref([])
const loading = ref(false)
const versionDialogVisible = ref(false)
const currentTransaction = ref(null)
const versionList = ref([])

onMounted(() => {
  bankId.value = route.query.bankId ? Number(route.query.bankId) : null
  bankName.value = route.query.bankName || ''
  if (bankId.value) {
    loadTransactionList()
  }
})

const loadTransactionList = async () => {
  loading.value = true
  try {
    // 获取该银行的所有交易类型
    const typesRes = await transactionTypeApi.getByBankId(bankId.value)
    if (!typesRes.data.success) {
      ElMessage.error('加载交易类型列表失败')
      return
    }
    
    const transactionTypes = typesRes.data.data || []
    
    // 为每个交易类型获取当前版本的配置
    const transactionListWithConfigs = await Promise.all(
      transactionTypes.map(async (transactionType) => {
        // 获取当前版本的请求配置
        const requestRes = await configV2Api.getCurrentConfig(transactionType.id, 'REQUEST')
        const requestConfig = requestRes.data.success && requestRes.data.data ? requestRes.data.data : null
        
        // 获取当前版本的响应配置
        const responseRes = await configV2Api.getCurrentConfig(transactionType.id, 'RESPONSE')
        const responseConfig = responseRes.data.success && responseRes.data.data ? responseRes.data.data : null
        
        // 从当前配置中获取版本号（优先使用请求配置的版本，如果没有则使用响应配置的版本）
        let currentVersion = 'v1'
        if (requestConfig && requestConfig.version) {
          currentVersion = requestConfig.version
        } else if (responseConfig && responseConfig.version) {
          currentVersion = responseConfig.version
        }
        
        return {
          id: transactionType.id,
          transactionName: transactionType.transactionName,
          description: transactionType.description,
          enabled: transactionType.enabled,
          requestConfig,
          responseConfig,
          currentVersion
        }
      })
    )
    
    transactionList.value = transactionListWithConfigs
  } catch (e) {
    console.error('加载交易类型列表失败:', e)
    ElMessage.error('加载交易类型列表失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push({ path: '/config' })
}

// 版本比较函数
const compareVersion = (v1, v2) => {
  const num1 = parseInt(v1.replace('v', '')) || 0
  const num2 = parseInt(v2.replace('v', '')) || 0
  return num1 - num2
}

const editConfig = (transactionTypeId, configType) => {
  if (configType) {
    // 编辑指定类型的配置
    router.push({
      path: '/canvas',
      query: {
        bankId: bankId.value,
        bankName: bankName.value,
        transactionTypeId: transactionTypeId,
        configType: configType
      }
    })
  } else {
    // 新增或编辑（让用户在编辑页面选择）
    router.push({
      path: '/canvas',
      query: {
        bankId: bankId.value,
        bankName: bankName.value,
        transactionTypeId: transactionTypeId
      }
    })
  }
}

// 显示版本管理对话框
const showVersionDialog = async (transaction) => {
  currentTransaction.value = transaction
  versionDialogVisible.value = true
  
  try {
    const res = await configV2Api.getVersions(transaction.id)
    if (res.data.success) {
      const versions = res.data.data || []
      
      // 获取每个版本的配置状态
      const versionInfoList = await Promise.all(
        versions.map(async (version) => {
          const requestRes = await configV2Api.getConfigByVersion(
            transaction.id, 
            'REQUEST', 
            version
          )
          const responseRes = await configV2Api.getConfigByVersion(
            transaction.id, 
            'RESPONSE', 
            version
          )
          
          return {
            version,
            hasRequest: requestRes.data.success && requestRes.data.data !== null,
            hasResponse: responseRes.data.success && responseRes.data.data !== null
          }
        })
      )
      
      versionList.value = versionInfoList
    }
  } catch (e) {
    console.error('加载版本列表失败:', e)
    ElMessage.error('加载版本列表失败')
  }
}

// 查看指定版本
const viewVersion = (version, configType) => {
  router.push({
    path: '/canvas',
    query: {
      bankId: bankId.value,
      bankName: bankName.value,
      transactionTypeId: currentTransaction.value.id,
      configType: configType,
      version: version
    }
  })
  versionDialogVisible.value = false
}

// 切换到指定版本
const switchVersion = async (version) => {
  try {
    await ElMessageBox.confirm(
      `确定要切换到版本 ${version} 吗？该版本将成为当前使用的版本。`,
      '确认切换',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await configV2Api.rollbackToVersion(
      currentTransaction.value.id,
      version
    )
    
    if (res.data.success) {
      ElMessage.success('版本切换成功')
      versionDialogVisible.value = false
      loadTransactionList() // 重新加载列表
    } else {
      ElMessage.error(res.data.errorMessage || '切换失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('切换版本失败:', e)
      ElMessage.error('切换失败: ' + (e.message || '未知错误'))
    }
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

