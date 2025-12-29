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
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <h3 style="margin: 0;">源数据</h3>
            <el-button 
              size="small" 
              type="success"
              @click="generateRandomData"
              :disabled="!testSourceData.trim()"
              title="根据当前数据结构生成随机测试数据"
            >
              <el-icon><Plus /></el-icon>
              生成随机数据
            </el-button>
          </div>
          <el-input
            v-model="testSourceData"
            type="textarea"
            :rows="10"
            placeholder="请输入源数据（JSON或XML），然后可以点击生成随机数据按钮自动填充测试数据"
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

// 生成随机数据
const generateRandomData = () => {
  if (!testSourceData.value.trim()) {
    ElMessage.warning('请先输入源数据结构')
    return
  }
  
  try {
    let data
    // 解析现有数据获取结构
    if (rule.value.sourceType === 'XML') {
      const parser = new DOMParser()
      const xmlDoc = parser.parseFromString(testSourceData.value, 'text/xml')
      const parseError = xmlDoc.querySelector('parsererror')
      if (parseError) {
        ElMessage.error('XML格式错误，无法生成随机数据')
        return
      }
      data = parseXmlToObject(testSourceData.value)
    } else {
      data = JSON.parse(testSourceData.value)
    }
    
    // 生成 Mock 数据
    const mockData = generateMockDataFromObject(data)
    
    // 格式化输出
    if (rule.value.sourceType === 'JSON') {
      testSourceData.value = JSON.stringify(mockData, null, 2)
    } else {
      testSourceData.value = objectToXml(mockData)
    }
    
    ElMessage.success('随机数据生成成功')
  } catch (e) {
    ElMessage.error('生成随机数据失败: ' + e.message)
    console.error('生成随机数据错误:', e)
  }
}

// 从对象生成 Mock 数据
const generateMockDataFromObject = (obj) => {
  if (Array.isArray(obj)) {
    const length = Math.min(obj.length, 3) // 最多3个元素
    return Array.from({ length }, (_, i) => {
      if (obj[i] && typeof obj[i] === 'object') {
        return generateMockDataFromObject(obj[i])
      }
      return generateRandomValueByType(typeof obj[i], '')
    })
  } else if (obj && typeof obj === 'object') {
    const result = {}
    Object.keys(obj).forEach(key => {
      const value = obj[key]
      if (Array.isArray(value)) {
        result[key] = generateMockDataFromObject(value)
      } else if (value && typeof value === 'object') {
        result[key] = generateMockDataFromObject(value)
      } else {
        result[key] = generateRandomValueByType(typeof value, key)
      }
    })
    return result
  } else {
    return generateRandomValueByType(typeof obj, '')
  }
}

// 根据类型和字段名生成随机值
const generateRandomValueByType = (type, fieldName = '') => {
  const lowerFieldName = fieldName.toLowerCase()
  
  // 根据字段名推断类型（更智能的生成）
  if (lowerFieldName.includes('id') || lowerFieldName.includes('code')) {
    return Math.floor(Math.random() * 1000000).toString()
  }
  if (lowerFieldName.includes('name') || lowerFieldName.includes('title')) {
    const names = ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十']
    return names[Math.floor(Math.random() * names.length)]
  }
  if (lowerFieldName.includes('email')) {
    const domains = ['example.com', 'test.com', 'demo.com']
    return `user${Math.floor(Math.random() * 1000)}@${domains[Math.floor(Math.random() * domains.length)]}`
  }
  if (lowerFieldName.includes('phone') || lowerFieldName.includes('mobile')) {
    return `1${Math.floor(Math.random() * 9) + 1}${String(Math.floor(Math.random() * 100000000)).padStart(9, '0')}`
  }
  if (lowerFieldName.includes('address') || lowerFieldName.includes('addr')) {
    const addresses = ['北京市朝阳区', '上海市浦东新区', '广州市天河区', '深圳市南山区', '杭州市西湖区']
    return addresses[Math.floor(Math.random() * addresses.length)]
  }
  if (lowerFieldName.includes('date') || lowerFieldName.includes('time')) {
    const now = new Date()
    const daysAgo = Math.floor(Math.random() * 365)
    const date = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000)
    return date.toISOString().split('T')[0]
  }
  if (lowerFieldName.includes('age')) {
    return Math.floor(Math.random() * 50) + 18
  }
  if (lowerFieldName.includes('price') || lowerFieldName.includes('amount') || lowerFieldName.includes('money')) {
    return parseFloat((Math.random() * 10000).toFixed(2))
  }
  if (lowerFieldName.includes('status') || lowerFieldName.includes('state')) {
    const statuses = ['active', 'inactive', 'pending', 'completed', 'cancelled']
    return statuses[Math.floor(Math.random() * statuses.length)]
  }
  if (lowerFieldName.includes('flag') || lowerFieldName.includes('enabled') || lowerFieldName.includes('disabled')) {
    return Math.random() > 0.5
  }
  
  // 根据类型生成
  switch (type) {
    case 'string':
      const strings = ['示例文本', '测试数据', '随机内容', 'Mock值', '示例值']
      return strings[Math.floor(Math.random() * strings.length)] + Math.floor(Math.random() * 100)
    case 'number':
      return Math.floor(Math.random() * 1000)
    case 'boolean':
      return Math.random() > 0.5
    default:
      return 'mock_value'
  }
}

// XML转对象
const parseXmlToObject = (xmlString) => {
  try {
    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml')
    
    const parseError = xmlDoc.querySelector('parsererror')
    if (parseError) {
      throw new Error('XML解析错误: ' + parseError.textContent)
    }
    
    const parseNode = (node) => {
      const elementChildren = []
      let textContent = ''
      
      for (let i = 0; i < node.childNodes.length; i++) {
        const child = node.childNodes[i]
        if (child.nodeType === 3) {
          const text = child.textContent?.trim()
          if (text) {
            textContent += (textContent ? ' ' : '') + text
          }
        } else if (child.nodeType === 1) {
          elementChildren.push(child)
        }
      }
      
      if (elementChildren.length > 0) {
        const result = {}
        if (node.attributes && node.attributes.length > 0) {
          for (let i = 0; i < node.attributes.length; i++) {
            const attr = node.attributes[i]
            result['@' + attr.name] = attr.value
          }
        }
        elementChildren.forEach(child => {
          const childName = child.nodeName
          const childValue = parseNode(child)
          if (result[childName]) {
            if (!Array.isArray(result[childName])) {
              result[childName] = [result[childName]]
            }
            result[childName].push(childValue)
          } else {
            result[childName] = childValue
          }
        })
        if (textContent) {
          result['#text'] = textContent
        }
        return result
      } else if (textContent) {
        return textContent
      } else {
        return {}
      }
    }
    
    const rootElement = xmlDoc.documentElement
    const rootValue = parseNode(rootElement)
    
    if (typeof rootValue === 'object' && rootValue !== null) {
      return rootValue
    }
    
    const rootName = rootElement.nodeName
    const rootObj = {}
    rootObj[rootName] = rootValue
    return rootObj
  } catch (e) {
    throw new Error('XML解析失败: ' + e.message)
  }
}

// 对象转 XML
const objectToXml = (obj, rootName = 'root') => {
  const convert = (data, tagName) => {
    if (Array.isArray(data)) {
      return data.map(item => convert(item, tagName)).join('\n')
    } else if (data && typeof data === 'object') {
      const keys = Object.keys(data)
      if (keys.length === 0) {
        return `<${tagName}></${tagName}>`
      }
      let xml = `<${tagName}>`
      keys.forEach(key => {
        if (key.startsWith('@') || key === '#text') {
          return // 跳过属性和文本节点
        }
        const value = data[key]
        if (Array.isArray(value)) {
          xml += '\n' + value.map(item => convert(item, key)).join('\n')
        } else if (value && typeof value === 'object') {
          xml += '\n' + convert(value, key)
        } else {
          xml += `\n  <${key}>${escapeXml(String(value))}</${key}>`
        }
      })
      xml += `\n</${tagName}>`
      return xml
    } else {
      return `<${tagName}>${escapeXml(String(data))}</${tagName}>`
    }
  }
  
  const rootKey = Object.keys(obj)[0] || rootName
  const xmlContent = convert(obj[rootKey] || obj, rootKey)
  return `<?xml version="1.0" encoding="UTF-8"?>\n${xmlContent}`
}

// XML 转义
const escapeXml = (str) => {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
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

