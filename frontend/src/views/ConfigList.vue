<template>
  <div class="config-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>配置管理</span>
        </div>
      </template>

      <el-table :data="bankList" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="银行类别" />
        <el-table-column label="已配置请求个数" width="150" align="center">
          <template #default="{ row }">
            {{ row.requestCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="是否启用" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewTransactions(row)">查看交易类型</el-button>
            <el-button 
              :type="row.enabled ? 'warning' : 'success'" 
              size="small" 
              @click="toggleBankEnabled(row)"
            >
              {{ row.enabled ? '禁用' : '启用' }}
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
import { ElMessage } from 'element-plus'
import { transactionTypeApi, bankApi } from '../api'

const router = useRouter()
const bankList = ref([])
const loading = ref(false)

onMounted(() => {
  loadBankList()
})

const loadBankList = async () => {
  loading.value = true
  try {
    // 获取所有银行列表（包括禁用的）
    const bankRes = await bankApi.getAllBanks()
    if (bankRes.data.success) {
      const banks = bankRes.data.data || []
      
      // 为每个银行统计已配置请求数量（通过交易类型统计）
      const banksWithCount = await Promise.all(
        banks.map(async (bank) => {
          try {
            // 获取该银行的所有交易类型
            const typesRes = await transactionTypeApi.getByBankId(bank.id)
            const transactionTypes = typesRes.data.success ? typesRes.data.data : []
            
            // 统计有请求配置的交易类型数量
            let requestCount = 0
            for (const transactionType of transactionTypes) {
              // 这里可以调用API检查是否有请求配置，但为了性能，暂时只统计交易类型数量
              // 实际应该调用configV2Api.getCurrentConfig来检查
              requestCount++
            }
            
            return {
              ...bank,
              requestCount: transactionTypes.length // 暂时使用交易类型数量
            }
          } catch (e) {
            console.error(`统计银行 ${bank.name} 的请求配置数量失败:`, e)
            return {
              ...bank,
              requestCount: 0
            }
          }
        })
      )
      
      bankList.value = banksWithCount
    }
  } catch (e) {
    console.error('加载银行列表失败:', e)
  } finally {
    loading.value = false
  }
}

const toggleBankEnabled = async (bank) => {
  try {
    // 这里需要调用银行API来切换启用状态
    // 由于银行信息管理在BankList页面，这里可以跳转过去
    // 或者我们可以添加一个快捷切换的API
    const res = await bankApi.saveBank(
      bank.id,
      bank.name,
      bank.code,
      bank.description,
      !bank.enabled
    )
    
    if (res.data.success) {
      ElMessage.success(bank.enabled ? '银行已禁用' : '银行已启用')
      loadBankList()
    } else {
      ElMessage.error(res.data.errorMessage || '操作失败')
    }
  } catch (e) {
    console.error('切换银行启用状态失败:', e)
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

const viewTransactions = (bank) => {
  router.push({ 
    path: '/config/transactions', 
    query: { bankId: bank.id, bankName: bank.name } 
  })
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
