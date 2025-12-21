<template>
  <div class="canvas-editor">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <span>报文映射画布编辑器</span>
          <div>
            <el-button @click="exportConfig">导出配置</el-button>
          </div>
        </div>
      </template>

      <div class="editor-container">
        <div class="source-panel">
          <h3>源数据</h3>
          <el-input
              v-model="sourceJson"
              type="textarea"
              :rows="10"
              placeholder="请输入源JSON数据"
              @input="parseSourceTree"
          />
          <el-divider />
          <div class="tree-tip">拖拽或双击字段到画布</div>
          <el-tree
              ref="sourceTreeRef"
              :data="sourceTreeData"
              :props="treeProps"
              node-key="path"
              :default-expand-all="true"
              class="data-tree"
          >
            <template #default="{ node, data }">
              <span
                  class="tree-node"
                  draggable="true"
                  @dragstart="handleTreeDragStart($event, data, 'source')"
                  @dragend="handleTreeDragEnd"
                  @dblclick="handleTreeNodeDoubleClick(data)"
              >
                <el-icon><Document /></el-icon>
                {{ node.label }} ({{ data.type }})
              </span>
            </template>
          </el-tree>
        </div>

        <div
            class="canvas-panel"
            @dragover.prevent
            @drop="handleCanvasDrop"
        >
          <div ref="graphContainer" class="graph-container"></div>
        </div>

        <div class="preview-panel">
          <h3>转换预览</h3>
          <el-button
              type="primary"
              size="small"
              @click="updatePreview"
              style="margin-bottom: 10px;"
              :loading="previewing"
          >
            刷新预览
          </el-button>
          <el-input
              v-model="previewResult"
              type="textarea"
              :rows="20"
              readonly
              placeholder="配置映射规则后，点击刷新预览"
              style="font-family: 'Courier New', monospace;"
          />
          <div v-if="previewError" style="margin-top: 10px; color: #f56c6c; font-size: 12px;">
            {{ previewError }}
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="nodeEditVisible" title="编辑目标字段" width="500px">
      <el-form :model="currentNodeEdit" label-width="100px">
        <el-form-item label="字段名"><el-input v-model="currentNodeEdit.fieldName" /></el-form-item>
        <el-form-item label="字段路径"><el-input v-model="currentNodeEdit.path" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNodeEdit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="edgeConfigVisible" title="配置转换规则" width="600px">
      <el-form :model="currentEdgeConfig" label-width="120px">
        <el-form-item label="源路径"><el-input v-model="currentEdgeConfig.sourcePath" disabled /></el-form-item>
        <el-form-item label="目标路径"><el-input v-model="currentEdgeConfig.targetPath" disabled /></el-form-item>
        <el-form-item label="映射类型">
          <el-select v-model="currentEdgeConfig.mappingType">
            <el-option label="1对1" value="ONE_TO_ONE" />
            <el-option label="1对多" value="ONE_TO_MANY" />
            <el-option label="多对1" value="MANY_TO_ONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="转换类型">
          <el-select v-model="currentEdgeConfig.transformType" @change="onTransformTypeChange">
            <el-option label="直接赋值" value="DIRECT" />
            <el-option label="函数映射" value="FUNCTION" />
            <el-option label="Groovy脚本" value="GROOVY" />
            <el-option label="字典映射" value="DICTIONARY" />
            <el-option label="固定值" value="FIXED" />
          </el-select>
        </el-form-item>
        
        <!-- 函数映射配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'FUNCTION'" label="函数名称">
          <el-select v-model="currentEdgeConfig.transformConfig.function">
            <el-option label="转大写" value="upperCase" />
            <el-option label="转小写" value="lowerCase" />
            <el-option label="去除空格" value="trim" />
            <el-option label="当前日期" value="currentDate" />
          </el-select>
        </el-form-item>
        
        <!-- Groovy脚本配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'GROOVY'" label="Groovy脚本">
          <el-input
            v-model="currentEdgeConfig.transformConfig.groovyScript"
            type="textarea"
            :rows="8"
            placeholder="输入Groovy脚本代码&#10;&#10;示例1 - 去掉邮箱@后面的部分:&#10;input?.toString()?.split('@')?[0] ?: ''&#10;&#10;示例2 - 字符串拼接(多对1):&#10;def parts = input as List; return (parts[0] ?: '') + ' ' + (parts[1] ?: '')&#10;&#10;可用变量: input(输入值), inputs(List类型时的别名)"
          />
          <div style="font-size: 12px; color: #999; margin-top: 5px;">
            <div><strong>变量说明：</strong></div>
            <div>• input: 输入的字段值（单个值或List）</div>
            <div>• inputs: 当输入是List时的别名</div>
          </div>
        </el-form-item>
        
        <!-- 字典映射配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'DICTIONARY'" label="字典映射">
          <div class="dict-config">
            <div v-for="(key, index) in dictKeys" :key="index" class="dict-item">
              <el-input v-model="dictKeys[index]" placeholder="源值" style="width: 45%" />
              <span style="margin: 0 10px">-></span>
              <el-input v-model="dictValues[index]" placeholder="目标值" style="width: 45%" />
              <el-button link type="danger" @click="removeDictItem(index)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-button @click="addDictItem" style="margin-top: 10px">添加映射项</el-button>
          </div>
        </el-form-item>
        
        <!-- 固定值配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'FIXED'" label="固定值">
          <el-input v-model="currentEdgeConfig.transformConfig.fixedValue" placeholder="请输入固定值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdgeConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { Graph } from '@antv/x6'
import { Selection } from '@antv/x6-plugin-selection'
import { ElMessage } from 'element-plus'
import { Document, Delete } from '@element-plus/icons-vue'
import { transformV2Api } from '../api'

// --- 状态变量 ---
const sourceJson = ref('')
const sourceTreeData = ref([])
const graphContainer = ref(null)
let graph = null
let nodeCounter = 0
let autoLayoutCount = 0 // 用于双击排版的计数器

const previewResult = ref('')
const previewError = ref('')
const previewing = ref(false)
let keyDownHandler = null

const treeProps = { children: 'children', label: 'label' }
const edgeConfigVisible = ref(false)
const nodeEditVisible = ref(false)
let currentNodeToEdit = null
const currentNodeEdit = ref({ fieldName: '', path: '' })
const currentEdgeConfig = ref({ 
  sourcePath: '', 
  targetPath: '', 
  mappingType: 'ONE_TO_ONE',
  transformType: 'DIRECT', 
  transformConfig: {} 
})
const dictKeys = ref([])
const dictValues = ref([])

onMounted(() => {
  nextTick(() => initGraph())
})

onUnmounted(() => {
  if (keyDownHandler) window.removeEventListener('keydown', keyDownHandler)
  if (graph) {
    graph.dispose()
    graph = null
  }
})

const initGraph = () => {
  if (!graphContainer.value) return

  graph = new Graph({
    container: graphContainer.value,
    grid: true,
    panning: {
      enabled: true,
      eventTypes: ['rightMouseDown'],
    },
    mousewheel: {
      enabled: true,
      zoomAtMousePosition: true,
      modifiers: 'ctrl',
    },
    connecting: {
      router: 'manhattan',
      connector: { name: 'rounded' },
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      allowLoop: false,
      highlight: true,
      validateConnection({ sourceCell, targetCell }) {
        if (sourceCell && targetCell && sourceCell !== targetCell) {
          const s = sourceCell.getData()
          const t = targetCell.getData()
          return s?.type === 'source' && t?.type === 'target'
        }
        return false
      },
    },
  })

  graph.use(
      new Selection({
        enabled: true,
        multiple: true,
        rubberband: true,
        movable: true,
        showNodeSelectionBox: true,
        showEdgeSelectionBox: true,
      })
  )

  graph.on('node:dblclick', ({ node }) => {
    if (node.getData()?.type === 'target') openNodeEditDialog(node)
  })

  graph.on('edge:dblclick', ({ edge }) => {
    openEdgeConfig(edge)
  })

  // 监听各种操作自动刷新预览
  graph.on('edge:connected', () => nextTick(() => updatePreview()))
  graph.on('node:removed', () => nextTick(() => updatePreview()))
  graph.on('edge:removed', () => nextTick(() => updatePreview()))

  keyDownHandler = (e) => {
    if (e.key !== 'Delete' && e.key !== 'Backspace') return
    const active = document.activeElement
    if (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA') return

    const selected = graph.getSelectedCells()
    if (selected.length > 0) {
      e.preventDefault()
      graph.removeCells(selected)
    }
  }
  window.addEventListener('keydown', keyDownHandler)
}

// --- 树逻辑 ---
const parseSourceTree = () => {
  try {
    if (!sourceJson.value.trim()) { sourceTreeData.value = []; return }
    const data = JSON.parse(sourceJson.value)
    sourceTreeData.value = [convertToTreeData(data, 'source', '$')]
  } catch (e) { sourceTreeData.value = [] }
}

const convertToTreeData = (obj, prefix, pathPrefix) => {
  const traverse = (data, parentPath = pathPrefix) => {
    const children = []
    if (Array.isArray(data)) {
      data.forEach((item, index) => {
        const path = `${parentPath}[${index}]`
        const node = { label: `[${index}]`, path, type: typeof item }
        if (item && typeof item === 'object') node.children = traverse(item, path)
        children.push(node)
      })
    } else if (data && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        const path = parentPath === '$' ? `$.${key}` : `${parentPath}.${key}`
        const value = data[key]
        const node = { label: key, path, type: typeof value }
        if (value && typeof value === 'object') node.children = traverse(value, path)
        children.push(node)
      })
    }
    return children
  }
  return { label: 'Source Root', path: pathPrefix, type: typeof obj, children: traverse(obj) }
}

// --- 拖拽与双击逻辑 ---
let draggingData = null
const handleTreeDragStart = (e, data) => { draggingData = data }
const handleTreeDragEnd = () => { draggingData = null }

/**
 * 双击树节点：计算排版位置并自动添加
 */
const handleTreeNodeDoubleClick = (data) => {
  if (!graph) return
  const rect = graphContainer.value.getBoundingClientRect()

  // 智能排版算法：每 10 个换一列
  const ROW_HEIGHT = 60
  const COLUMN_WIDTH = 450
  const row = autoLayoutCount % 10
  const col = Math.floor(autoLayoutCount / 10)

  // 基础起始点：x=250 (留出源节点空间), y=50
  const targetX = 250 + (col * COLUMN_WIDTH)
  const targetY = 50 + (row * ROW_HEIGHT)

  draggingData = data
  const mockEvent = {
    preventDefault: () => {},
    clientX: rect.left + targetX,
    clientY: rect.top + targetY
  }

  handleCanvasDrop(mockEvent)
  autoLayoutCount++ // 计数器加1，确保下次不重叠
  draggingData = null
}

/**
 * 画布放置逻辑：处理手动拖拽和双击生成的坐标
 */
const handleCanvasDrop = (event) => {
  if (!graph || !draggingData) return
  const rect = graphContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const fieldName = draggingData.label
  const sId = `s_${++nodeCounter}`, tId = `t_${++nodeCounter}`

  // 1. 源节点
  graph.addNode({
    id: sId, x: x - 200, y: y - 25, width: 140, height: 40, label: fieldName,
    data: { type: 'source', path: draggingData.path },
    ports: {
      groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'right' }]
    },
    attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
  })

  // 2. 目标节点
  graph.addNode({
    id: tId, x: x + 50, y: y - 25, width: 140, height: 40, label: fieldName,
    data: { type: 'target', path: fieldName },
    ports: {
      groups: { left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#9c27b0', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'left' }]
    },
    attrs: { body: { fill: '#f3e5f5', stroke: '#9c27b0', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
  })

  // 3. 连线
  graph.addEdge({
    source: { cell: sId, port: 'p1' },
    target: { cell: tId, port: 'p1' },
    attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
    data: { mappingType: 'ONE_TO_ONE', transformType: 'DIRECT', transformConfig: {} },
    labels: [{ attrs: { text: { text: 'DIRECT', fontSize: 10 } } }]
  })

  // 4. 自动刷新预览
  nextTick(() => updatePreview())
}

// --- 配置与转换逻辑 ---
const openNodeEditDialog = (node) => {
  currentNodeToEdit = node
  const data = node.getData()
  currentNodeEdit.value = { fieldName: node.attr('text/text'), path: data.path }
  nodeEditVisible.value = true
}

const saveNodeEdit = () => {
  currentNodeToEdit.attr('text/text', currentNodeEdit.value.fieldName)
  currentNodeToEdit.setData({ ...currentNodeToEdit.getData(), path: currentNodeEdit.value.path })
  nodeEditVisible.value = false
  updatePreview()
}

const openEdgeConfig = (edge) => {
  const s = graph.getCellById(edge.getSourceCellId()), t = graph.getCellById(edge.getTargetCellId())
  if (!s || !t) return
  currentEdge = edge
  const data = edge.getData() || {}
  currentEdgeConfig.value = {
    sourcePath: s.getData().path,
    targetPath: t.getData().path,
    mappingType: data.mappingType || 'ONE_TO_ONE',
    transformType: data.transformType || 'DIRECT',
    transformConfig: data.transformConfig || {}
  }
  
  // 初始化字典配置
  if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    const dict = currentEdgeConfig.value.transformConfig?.dictionary || {}
    dictKeys.value = Object.keys(dict)
    dictValues.value = Object.values(dict)
    if (dictKeys.value.length === 0) {
      dictKeys.value = ['']
      dictValues.value = ['']
    }
  } else {
    dictKeys.value = []
    dictValues.value = []
  }
  
  edgeConfigVisible.value = true
}

let currentEdge = null
const onTransformTypeChange = () => {
  // 切换转换类型时，清空旧的配置
  currentEdgeConfig.value.transformConfig = {}
  if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    // 初始化一个空的映射项
    dictKeys.value = ['']
    dictValues.value = ['']
  } else {
    dictKeys.value = []
    dictValues.value = []
  }
}
const addDictItem = () => { 
  dictKeys.value.push('')
  dictValues.value.push('') 
}
const removeDictItem = (i) => { 
  dictKeys.value.splice(i, 1)
  dictValues.value.splice(i, 1) 
}

const saveEdgeConfig = () => {
  if (!currentEdge) return
  
  // 确保transformConfig存在
  const config = { ...currentEdgeConfig.value.transformConfig }
  
  // 根据转换类型保存不同的配置
  if (currentEdgeConfig.value.transformType === 'FUNCTION') {
    // 函数映射：确保function字段存在
    if (!config.function) {
      config.function = 'upperCase' // 默认值
    }
  } else if (currentEdgeConfig.value.transformType === 'GROOVY') {
    // Groovy脚本：确保groovyScript字段存在
    if (!config.groovyScript) {
      config.groovyScript = 'return input'
    }
  } else if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    // 字典映射：保存字典键值对
    const dict = {}
    dictKeys.value.forEach((k, i) => { 
      if (k && dictValues.value[i]) {
        dict[k] = dictValues.value[i] 
      }
    })
    config.dictionary = dict
  } else if (currentEdgeConfig.value.transformType === 'FIXED') {
    // 固定值：确保fixedValue字段存在
    if (config.fixedValue === undefined) {
      config.fixedValue = ''
    }
  }
  
  // 更新边的数据
  currentEdge.setData({
    ...currentEdge.getData(),
    mappingType: currentEdgeConfig.value.mappingType,
    transformType: currentEdgeConfig.value.transformType,
    transformConfig: config
  })
  
  // 更新边的标签显示
  currentEdge.setLabels([{
    attrs: {
      text: {
        text: currentEdgeConfig.value.transformType,
        fill: '#666',
        fontSize: 10,
      }
    }
  }])
  
  edgeConfigVisible.value = false
  currentEdge = null
  ElMessage.success('配置保存成功')
  
  // 配置保存后自动更新预览
  nextTick(() => {
    updatePreview()
  })
}

const updatePreview = async () => {
  if (!sourceJson.value || previewing.value) return
  const config = exportMappingConfig()
  if (!config.rules.length) return
  previewing.value = true
  try {
    const res = await transformV2Api.transform(sourceJson.value, config)
    previewResult.value = JSON.stringify(JSON.parse(res.data.transformedData), null, 2)
    previewError.value = ''
  } catch (e) {
    previewError.value = '转换失败，请检查配置或脚本'
  } finally { previewing.value = false }
}

const exportMappingConfig = () => {
  const rules = graph.getEdges().map(edge => {
    const s = graph.getCellById(edge.getSourceCellId())
    const t = graph.getCellById(edge.getTargetCellId())
    const data = edge.getData() || {}
    if (!s || !t) return null
    
    let sourcePath = s.getData().path
    if (sourcePath && !sourcePath.startsWith('$')) {
      sourcePath = `$.${sourcePath.replace(/^\./, '')}`
    }
    
    return {
      sourcePath: sourcePath,
      targetPath: t.getData().path,
      mappingType: data.mappingType || 'ONE_TO_ONE',
      transformType: data.transformType || 'DIRECT',
      transformConfig: data.transformConfig || {}
    }
  }).filter(r => r)
  
  return {
    sourceProtocol: 'JSON',
    targetProtocol: 'JSON',
    prettyPrint: true,
    rules: rules
  }
}

const exportConfig = () => {
  const config = exportMappingConfig()
  navigator.clipboard.writeText(JSON.stringify(config, null, 2))
  ElMessage.success('配置已复制')
}
</script>

<style scoped>
.canvas-editor { padding: 20px; }
.editor-card { height: calc(100vh - 100px); }
.editor-container { display: flex; gap: 20px; height: calc(100vh - 200px); }
.source-panel { width: 300px; display: flex; flex-direction: column; }
.canvas-panel { flex: 1; border: 1px solid #ddd; border-radius: 4px; overflow: hidden; position: relative; }
.graph-container { width: 100%; height: 100%; background-color: #fafafa; }
.preview-panel { width: 350px; display: flex; flex-direction: column; }
.data-tree { flex: 1; overflow: auto; border: 1px solid #ddd; padding: 10px; }
.dict-item { display: flex; align-items: center; margin-bottom: 8px; }

:deep(.x6-node-selected) rect {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}
:deep(.x6-edge-selected) path {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}
</style>