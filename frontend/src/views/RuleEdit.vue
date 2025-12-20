<template>
  <div class="rule-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ ruleId ? '编辑规则' : '新建规则' }}</span>
          <div>
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" @click="saveRule" :loading="saving" :disabled="saving">保存</el-button>
          </div>
        </div>
      </template>
      
      <el-form :model="rule" label-width="120px">
        <el-form-item label="规则名称">
          <el-input v-model="rule.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="rule.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="源协议类型">
          <el-select v-model="rule.sourceType" placeholder="请选择">
            <el-option label="JSON" value="JSON" />
            <el-option label="XML" value="XML" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标协议类型">
          <el-select v-model="rule.targetType" placeholder="请选择">
            <el-option label="JSON" value="JSON" />
            <el-option label="XML" value="XML" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="rule.enabled" />
        </el-form-item>
      </el-form>
      
      <el-divider>字段映射配置</el-divider>
      
      <div class="mapping-config">
        <el-button type="primary" @click="addMapping">
          <el-icon><Plus /></el-icon>
          添加字段映射
        </el-button>
        
        <draggable 
          v-model="rule.fieldMappings" 
          item-key="id"
          handle=".drag-handle"
          class="mapping-list"
        >
          <template #item="{ element, index }">
            <FieldMappingCard 
              :mapping="element" 
              :index="index"
              @update="updateMapping(index, $event)"
              @delete="deleteMapping(index)"
            />
          </template>
        </draggable>
      </div>
    </el-card>
    
    <!-- 转换测试 -->
    <el-card class="test-card">
      <template #header>
        <span>转换测试</span>
      </template>
      <div class="test-container">
        <div class="test-input">
          <h3>源数据</h3>
          <el-input
            v-model="testSourceData"
            type="textarea"
            :rows="10"
            placeholder="请输入源数据（JSON或XML）"
          />
        </div>
        <div class="test-actions">
          <el-button type="primary" @click="testTransform" :loading="transforming">
            执行转换
          </el-button>
        </div>
        <div class="test-output">
          <h3>转换结果</h3>
          <el-input
            v-model="testResult"
            type="textarea"
            :rows="10"
            readonly
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import FieldMappingCard from '../components/FieldMappingCard.vue'
import { ruleApi } from '../api'

const route = useRoute()
const router = useRouter()
const ruleId = ref(route.params.id)

const rule = ref({
  name: '',
  description: '',
  sourceType: 'JSON',
  targetType: 'JSON',
  enabled: true,
  fieldMappings: []
})

const testSourceData = ref('')
const testResult = ref('')
const transforming = ref(false)

onMounted(async () => {
  if (ruleId.value) {
    await loadRule()
  }
})

const loadRule = async () => {
  try {
    const response = await ruleApi.getRuleById(ruleId.value)
    rule.value = response.data
    // 转换枚举值为字符串
    if (rule.value.sourceType && typeof rule.value.sourceType === 'object') {
      rule.value.sourceType = rule.value.sourceType.name
    }
    if (rule.value.targetType && typeof rule.value.targetType === 'object') {
      rule.value.targetType = rule.value.targetType.name
    }
  } catch (error) {
    ElMessage.error('加载规则失败: ' + error.message)
  }
}

const addMapping = () => {
  rule.value.fieldMappings.push({
    id: Date.now().toString(),
    mappingType: 'ONE_TO_ONE',
    sourceField: '',
    targetField: '',
    transformType: 'EQUAL'
  })
}

const updateMapping = (index, mapping) => {
  rule.value.fieldMappings[index] = mapping
}

const deleteMapping = (index) => {
  rule.value.fieldMappings.splice(index, 1)
}

const saving = ref(false)

const saveRule = async () => {
  // 防止重复保存
  if (saving.value) {
    return
  }
  
  saving.value = true
  try {
    // 深拷贝规则对象，避免直接修改原始对象
    const ruleToSave = JSON.parse(JSON.stringify(rule.value))
    const response = await ruleApi.saveRule(ruleToSave)
    ElMessage.success('保存成功')
    // 更新本地规则对象，确保ID被设置
    rule.value.id = response.data.id
    if (!ruleId.value) {
      router.push(`/rule/${response.data.id}`)
      ruleId.value = response.data.id
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const testTransform = async () => {
  if (!testSourceData.value.trim()) {
    ElMessage.warning('请输入源数据')
    return
  }
  
  // 如果规则没有ID，先保存规则
  if (!rule.value.id) {
    try {
      const saveResponse = await ruleApi.saveRule(JSON.parse(JSON.stringify(rule.value)))
      rule.value.id = saveResponse.data.id
      if (!ruleId.value) {
        ruleId.value = saveResponse.data.id
        // 更新URL但不触发导航
        window.history.replaceState(null, '', `/rule/${saveResponse.data.id}`)
      }
    } catch (error) {
      ElMessage.error('保存规则失败: ' + error.message)
      return
    }
  }
  
  if (rule.value.fieldMappings.length === 0) {
    ElMessage.warning('请先配置字段映射')
    return
  }
  
  transforming.value = true
  try {
    const response = await ruleApi.transform(rule.value.id, testSourceData.value, true)
    if (response.data.success) {
      testResult.value = response.data.transformedData
    } else {
      ElMessage.error('转换失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    ElMessage.error('转换失败: ' + error.message)
  } finally {
    transforming.value = false
  }
}

const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.rule-edit {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mapping-config {
  margin-top: 20px;
}

.mapping-list {
  margin-top: 20px;
}

.test-card {
  margin-top: 20px;
}

.test-container {
  display: flex;
  gap: 20px;
}

.test-input,
.test-output {
  flex: 1;
}

.test-actions {
  display: flex;
  align-items: center;
}

.test-actions .el-button {
  white-space: nowrap;
}
</style>

