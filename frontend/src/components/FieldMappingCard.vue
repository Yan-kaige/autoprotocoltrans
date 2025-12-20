<template>
  <el-card class="mapping-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <div class="drag-handle" style="cursor: move;">
          <el-icon><Rank /></el-icon>
          <span>字段映射 {{ index + 1 }}</span>
        </div>
        <el-button link type="danger" @click="$emit('delete')">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </template>
    
    <el-form :model="mapping" label-width="120px" size="small">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="映射类型">
            <el-select v-model="mapping.mappingType" @change="onMappingTypeChange">
              <el-option label="1对1" value="ONE_TO_ONE" />
              <el-option label="1对多" value="ONE_TO_MANY" />
              <el-option label="多对1" value="MANY_TO_ONE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="转换类型">
            <el-select v-model="mapping.transformType" @change="onTransformTypeChange">
              <el-option label="等于" value="EQUAL" />
              <el-option label="函数映射" value="FUNCTION" />
              <el-option label="Java方法映射" value="JAVA_METHOD" />
              <el-option label="数据类型转换" value="DATA_TYPE" />
              <el-option label="忽略" value="IGNORE" />
              <el-option label="字典映射" value="DICT" />
              <el-option label="固定值" value="FIXED" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="源字段路径">
            <el-input 
              v-model="mapping.sourceField" 
              placeholder="如: user.name 或 user.address.city"
              @change="emitUpdate"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="目标字段路径">
            <el-input 
              v-model="mapping.targetField" 
              placeholder="如: customer.name"
              @change="emitUpdate"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <!-- 函数映射配置 -->
      <el-form-item v-if="mapping.transformType === 'FUNCTION'" label="函数名称">
        <el-select v-model="mapping.functionName" @change="emitUpdate">
          <el-option 
            v-for="(desc, name) in availableFunctions" 
            :key="name"
            :label="`${name} (${desc})`"
            :value="name"
          />
        </el-select>
      </el-form-item>
      
      <!-- 数据类型转换配置 -->
      <el-form-item v-if="mapping.transformType === 'DATA_TYPE'" label="目标数据类型">
        <el-select v-model="mapping.dataType" @change="emitUpdate">
          <el-option label="字符串" value="string" />
          <el-option label="整数" value="int" />
          <el-option label="长整数" value="long" />
          <el-option label="浮点数" value="double" />
          <el-option label="布尔值" value="boolean" />
        </el-select>
      </el-form-item>
      
      <!-- 固定值配置 -->
      <el-form-item v-if="mapping.transformType === 'FIXED'" label="固定值">
        <el-input v-model="mapping.fixedValue" @change="emitUpdate" />
      </el-form-item>
      
      <!-- 字典映射配置 -->
      <el-form-item v-if="mapping.transformType === 'DICT'" label="字典映射">
        <div class="dict-mapping">
          <div 
            v-for="(value, key, idx) in dictMapping" 
            :key="idx"
            class="dict-item"
          >
            <el-input v-model="dictMapping[key]" placeholder="源值" style="width: 45%" />
            <span style="margin: 0 10px">-></span>
            <el-input 
              v-model="dictMappingValue[key]" 
              placeholder="目标值" 
              style="width: 45%"
            />
            <el-button link type="danger" @click="removeDictItem(key)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button @click="addDictItem">添加映射项</el-button>
        </div>
      </el-form-item>
      
      <!-- 1对多或多对1的子映射 -->
      <div v-if="mapping.mappingType === 'ONE_TO_MANY' || mapping.mappingType === 'MANY_TO_ONE'">
        <el-divider>子映射配置</el-divider>
        <el-button size="small" @click="addSubMapping">添加子映射</el-button>
        <div v-for="(sub, idx) in subMappings" :key="idx" class="sub-mapping">
          <el-input v-model="sub.sourceField" placeholder="源字段" style="width: 40%" />
          <span style="margin: 0 10px">-></span>
          <el-input v-model="sub.targetField" placeholder="目标字段" style="width: 40%" />
          <el-button link type="danger" @click="removeSubMapping(idx)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Rank, Delete } from '@element-plus/icons-vue'
import { functionApi } from '../api'

const props = defineProps({
  mapping: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['update', 'delete'])

const availableFunctions = ref({})
const dictMapping = ref({})
const dictMappingValue = ref({})
const subMappings = ref([])

onMounted(async () => {
  await loadFunctions()
  initMappingData()
})

const loadFunctions = async () => {
  try {
    const response = await functionApi.getAvailableFunctions()
    availableFunctions.value = response.data
  } catch (error) {
    console.error('加载函数列表失败:', error)
  }
}

const initMappingData = () => {
  // 初始化字典映射
  if (props.mapping.dictMapping) {
    dictMapping.value = { ...props.mapping.dictMapping }
    dictMappingValue.value = {}
    Object.keys(props.mapping.dictMapping).forEach(key => {
      dictMappingValue.value[key] = props.mapping.dictMapping[key]
    })
  }
  
  // 初始化子映射
  if (props.mapping.subMappings) {
    subMappings.value = [...props.mapping.subMappings]
  }
}

const onMappingTypeChange = () => {
  if (props.mapping.mappingType === 'ONE_TO_MANY' || props.mapping.mappingType === 'MANY_TO_ONE') {
    if (!props.mapping.subMappings) {
      props.mapping.subMappings = []
    }
    subMappings.value = props.mapping.subMappings
  }
  emitUpdate()
}

const onTransformTypeChange = () => {
  emitUpdate()
}

const addDictItem = () => {
  const key = `key${Object.keys(dictMapping.value).length + 1}`
  dictMapping.value[key] = ''
  dictMappingValue.value[key] = ''
  emitUpdate()
}

const removeDictItem = (key) => {
  delete dictMapping.value[key]
  delete dictMappingValue.value[key]
  emitUpdate()
}

const addSubMapping = () => {
  if (!props.mapping.subMappings) {
    props.mapping.subMappings = []
  }
  props.mapping.subMappings.push({
    sourceField: '',
    targetField: ''
  })
  subMappings.value = props.mapping.subMappings
  emitUpdate()
}

const removeSubMapping = (index) => {
  props.mapping.subMappings.splice(index, 1)
  subMappings.value = props.mapping.subMappings
  emitUpdate()
}

const emitUpdate = () => {
  // 更新字典映射
  if (props.mapping.transformType === 'DICT') {
    const dict = {}
    Object.keys(dictMapping.value).forEach(key => {
      if (dictMapping.value[key] && dictMappingValue.value[key]) {
        dict[dictMapping.value[key]] = dictMappingValue.value[key]
      }
    })
    props.mapping.dictMapping = dict
  }
  
  // 更新子映射
  if (props.mapping.mappingType === 'ONE_TO_MANY' || props.mapping.mappingType === 'MANY_TO_ONE') {
    props.mapping.subMappings = subMappings.value
  }
  
  emit('update', { ...props.mapping })
}

// 监听字典映射变化
watch([dictMapping, dictMappingValue], () => {
  if (props.mapping.transformType === 'DICT') {
    emitUpdate()
  }
}, { deep: true })

// 监听子映射变化
watch(subMappings, () => {
  if (props.mapping.mappingType === 'ONE_TO_MANY' || props.mapping.mappingType === 'MANY_TO_ONE') {
    emitUpdate()
  }
}, { deep: true })
</script>

<style scoped>
.mapping-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.drag-handle {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dict-mapping {
  width: 100%;
}

.dict-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.sub-mapping {
  display: flex;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 10px;
}
</style>

