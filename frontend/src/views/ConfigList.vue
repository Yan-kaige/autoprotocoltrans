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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewTransactions(row.name)">查看交易类型</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { configApi, bankApi } from '../api'

const router = useRouter()
const bankList = ref([])
const loading = ref(false)

onMounted(() => {
  loadBankList()
})

const loadBankList = async () => {
  loading.value = true
  try {
    // 获取所有启用的银行列表
    const bankRes = await bankApi.getEnabledBanks()
    if (bankRes.data.success) {
      bankList.value = bankRes.data.data || []
    }
  } catch (e) {
    console.error('加载银行列表失败:', e)
  } finally {
    loading.value = false
  }
}

const viewTransactions = (bankCategory) => {
  router.push({ 
    path: '/config/transactions', 
    query: { bankCategory } 
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
