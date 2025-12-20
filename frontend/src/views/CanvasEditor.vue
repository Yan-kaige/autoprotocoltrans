<template>
  <div class="canvas-editor">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <span>报文映射画布编辑器</span>
          <div>
            <el-button @click="exportConfig">导出配置</el-button>
            <el-button type="primary" @click="testTransform">测试转换</el-button>
          </div>
        </div>
      </template>
      
      <div class="editor-container">
        <!-- 左侧：源数据树 -->
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
          <el-tree
            ref="sourceTreeRef"
            :data="sourceTreeData"
            :props="treeProps"
            node-key="path"
            class="data-tree"
          >
            <template #default="{ node, data }">
              <span 
                class="tree-node"
                draggable="true"
                @dragstart="handleTreeDragStart($event, data, 'source')"
                @dragend="handleTreeDragEnd"
              >
                <el-icon><Document /></el-icon>
                {{ node.label }} ({{ data.type }})
              </span>
            </template>
          </el-tree>
        </div>
        
        <!-- 中间：画布区域 -->
        <div 
          class="canvas-panel"
          @dragover.prevent
          @drop="handleCanvasDrop"
        >
          <div ref="graphContainer" class="graph-container"></div>
        </div>
        
        <!-- 右侧：目标数据树 -->
        <div class="target-panel">
          <h3>目标数据</h3>
          <el-input
            v-model="targetJson"
            type="textarea"
            :rows="10"
            placeholder="请输入目标JSON结构（用于参考），转换结果会显示在这里，可直接编辑"
            @input="parseTargetTree"
          />
          <el-divider />
          <el-tree
            ref="targetTreeRef"
            :data="targetTreeData"
            :props="treeProps"
            node-key="path"
            class="data-tree"
          >
            <template #default="{ node, data }">
              <span 
                class="tree-node"
                draggable="true"
                @dragstart="handleTreeDragStart($event, data, 'target')"
                @dragend="handleTreeDragEnd"
              >
                <el-icon><Document /></el-icon>
                {{ node.label }} ({{ data.type }})
              </span>
            </template>
          </el-tree>
        </div>
      </div>
    </el-card>
    
    <!-- 节点编辑对话框 -->
    <el-dialog
      v-model="nodeEditVisible"
      title="编辑目标字段"
      width="500px"
    >
      <el-form :model="currentNodeEdit" label-width="100px">
        <el-form-item label="字段名">
          <el-input v-model="currentNodeEdit.fieldName" placeholder="请输入字段名" />
        </el-form-item>
        <el-form-item label="字段路径">
          <el-input v-model="currentNodeEdit.path" placeholder="例如: user.name" />
          <div style="font-size: 12px; color: #999; margin-top: 5px;">
            字段路径，用于转换后的JSON结构
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNodeEdit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 连线配置对话框 -->
    <el-dialog
      v-model="edgeConfigVisible"
      title="配置转换规则"
      width="600px"
    >
      <el-form :model="currentEdgeConfig" label-width="120px">
        <el-form-item label="源路径">
          <el-input v-model="currentEdgeConfig.sourcePath" disabled />
        </el-form-item>
        <el-form-item label="目标路径">
          <el-input v-model="currentEdgeConfig.targetPath" disabled />
        </el-form-item>
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
            <div style="margin-top: 5px;"><strong>常用示例：</strong></div>
            <div>• 去掉邮箱@后面: <code style="background: #f5f5f5; padding: 2px 4px; border-radius: 2px;">input?.toString()?.split('@')?[0] ?: ''</code></div>
            <div>• 转大写: <code style="background: #f5f5f5; padding: 2px 4px; border-radius: 2px;">input?.toString()?.toUpperCase()</code></div>
            <div>• 字符串拼接: <code style="background: #f5f5f5; padding: 2px 4px; border-radius: 2px;">def parts = input as List; return parts[0] + ' ' + parts[1]</code></div>
          </div>
        </el-form-item>
        
        <!-- 字典映射配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'DICTIONARY'" label="字典映射">
          <div class="dict-config">
            <div
              v-for="(key, index) in dictKeys"
              :key="index"
              class="dict-item"
            >
              <el-input v-model="dictKeys[index]" placeholder="源值" style="width: 45%" />
              <span style="margin: 0 10px">-></span>
              <el-input v-model="dictValues[index]" placeholder="目标值" style="width: 45%" />
              <el-button link type="danger" @click="removeDictItem(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="addDictItem" style="margin-top: 10px">添加映射项</el-button>
          </div>
        </el-form-item>
        
        <!-- 固定值配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'FIXED'" label="固定值">
          <el-input v-model="currentEdgeConfig.transformConfig.fixedValue" />
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
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { Graph } from '@antv/x6'
import { ElMessage } from 'element-plus'
import { Document, Delete } from '@element-plus/icons-vue'
import { transformV2Api } from '../api'

const sourceJson = ref('')
const targetJson = ref('')
const sourceTreeData = ref([])
const targetTreeData = ref([])
const graphContainer = ref(null)
let graph = null
let keyDownHandler = null

const treeProps = {
  children: 'children',
  label: 'label'
}

const edgeConfigVisible = ref(false)
const nodeEditVisible = ref(false)
let currentNodeToEdit = null

const currentNodeEdit = ref({
  fieldName: '',
  path: ''
})

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
  nextTick(() => {
    initGraph()
  })
})

onUnmounted(() => {
  // 移除键盘事件监听
  if (keyDownHandler) {
    window.removeEventListener('keydown', keyDownHandler)
    keyDownHandler = null
  }
  
  if (graph) {
    graph.dispose()
    graph = null
  }
})

const initGraph = () => {
  if (!graphContainer.value) return
  
  graph = new Graph({
    container: graphContainer.value,
    width: graphContainer.value.clientWidth,
    height: graphContainer.value.clientHeight,
    grid: true,
    panning: true,
    mousewheel: {
      enabled: true,
      zoomAtMousePosition: true,
      modifiers: 'ctrl',
      minScale: 0.5,
      maxScale: 3,
    },
    connecting: {
      router: {
        name: 'manhattan',
      },
      connector: {
        name: 'rounded',
      },
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: true, // 允许连接到空白处
      allowLoop: false, // 不允许自环
      allowNode: true, // 允许连接到节点
      allowEdge: false, // 不允许连接到边
      highlight: true, // 高亮显示可连接的点
      snap: {
        radius: 20,
      },
      validateConnection: ({ sourceMagnet, targetMagnet, sourceCell, targetCell }) => {
        // 验证连接：源节点必须是source类型，目标节点必须是target类型
        if (sourceCell && targetCell && sourceCell !== targetCell) {
          const sourceData = sourceCell.getData()
          const targetData = targetCell.getData()
          return sourceData?.type === 'source' && targetData?.type === 'target'
        }
        return false
      },
    },
  })
  
  // 监听连线创建事件
  graph.on('edge:click', ({ edge }) => {
    openEdgeConfig(edge)
  })
  
  // 监听连线创建完成
  graph.on('edge:connected', ({ edge }) => {
    // 连线创建时，默认配置为直接赋值
    edge.setData({
      mappingType: 'ONE_TO_ONE',
      transformType: 'DIRECT',
      transformConfig: {}
    })
    
    // 添加标签显示转换类型
    edge.setLabels([{
      attrs: {
        text: {
          text: 'DIRECT',
          fill: '#666',
          fontSize: 10,
        }
      }
    }])
  })
  
  // 监听节点双击事件，启用文本编辑（仅目标节点可编辑）
  graph.on('node:dblclick', ({ node }) => {
    const nodeData = node.getData() || {}
    // 只有目标节点可以编辑
    if (nodeData.type === 'target') {
      openNodeEditDialog(node)
    }
  })
  
  // 监听节点删除
  graph.on('node:removed', ({ node }) => {
    // 节点删除时，自动删除相关的连线
    const edges = graph.getEdges()
    edges.forEach(edge => {
      const sourceId = edge.getSourceCellId()
      const targetId = edge.getTargetCellId()
      if (sourceId === node.id || targetId === node.id) {
        graph.removeEdge(edge.id)
      }
    })
  })
  
  // 启用键盘删除（使用window事件监听）
  keyDownHandler = (e) => {
    // 检查是否有选中的单元格
    if (graph && (e.key === 'Delete' || e.key === 'Backspace')) {
      const cells = graph.getSelectedCells()
      if (cells.length) {
        // 如果焦点不在输入框中，才执行删除
        const activeElement = document.activeElement
        const isInput = activeElement && (
          activeElement.tagName === 'INPUT' || 
          activeElement.tagName === 'TEXTAREA' ||
          activeElement.isContentEditable
        )
        if (!isInput) {
          e.preventDefault()
          graph.removeCells(cells)
        }
      }
    }
  }
  
  // 使用window监听键盘事件
  window.addEventListener('keydown', keyDownHandler)
  graphContainer.value.setAttribute('tabindex', '0') // 使其可聚焦
}

const parseSourceTree = () => {
  try {
    if (!sourceJson.value.trim()) {
      sourceTreeData.value = []
      return
    }
    const data = JSON.parse(sourceJson.value)
    sourceTreeData.value = [convertToTreeData(data, 'source', '$')]
  } catch (e) {
    console.error('解析源JSON失败:', e)
  }
}

const parseTargetTree = () => {
  try {
    if (!targetJson.value.trim()) {
      targetTreeData.value = []
      return
    }
    const data = JSON.parse(targetJson.value)
    targetTreeData.value = [convertToTreeData(data, 'target', '')]
  } catch (e) {
    console.error('解析目标JSON失败:', e)
  }
}

const convertToTreeData = (obj, prefix, pathPrefix) => {
  const traverse = (data, parentPath = pathPrefix) => {
    const children = []
    
    if (Array.isArray(data)) {
      data.forEach((item, index) => {
        const path = `${parentPath}[${index}]`
        const itemType = Array.isArray(item) ? 'Array' : (item !== null && typeof item === 'object' ? 'Object' : typeof item)
        const node = {
          label: `[${index}]`,
          path: path,
          type: itemType,
        }
        
        if (item !== null && typeof item === 'object') {
          node.children = traverse(item, path)
        }
        
        children.push(node)
      })
    } else if (data !== null && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        const path = parentPath === '$' || parentPath === '' 
          ? (parentPath === '$' ? `$.${key}` : key)
          : `${parentPath}.${key}`
        const value = data[key]
        const valueType = Array.isArray(value) ? 'Array' : (value !== null && typeof value === 'object' ? 'Object' : typeof value)
        
        const node = {
          label: key,
          path: path,
          type: valueType,
        }
        
        if (value !== null && typeof value === 'object') {
          node.children = traverse(value, path)
        }
        
        children.push(node)
      })
    }
    
    return children
  }
  
  const rootType = Array.isArray(obj) ? 'Array' : (obj !== null && typeof obj === 'object' ? 'Object' : typeof obj)
  
  return {
    label: prefix === 'source' ? 'Source Root' : 'Target Root',
    path: pathPrefix,
    type: rootType,
    children: traverse(obj, pathPrefix)
  }
}

let draggingData = null
let nodeCounter = 0

const handleTreeDragStart = (event, data, type) => {
  draggingData = { ...data, nodeType: type }
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('application/json', JSON.stringify(draggingData))
}

const handleTreeDragEnd = () => {
  draggingData = null
}

const handleCanvasDrop = (event) => {
  event.preventDefault()
  
  if (!graph || !draggingData) return
  
  const rect = graphContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 计算节点位置（考虑画布的缩放和平移）
  const clientX = event.clientX - rect.left
  const clientY = event.clientY - rect.top
  
  nodeCounter++
  
  // 提取字段名（从路径中提取最后一个字段）
  const fieldName = draggingData.path 
    ? draggingData.path.split('.').pop().replace(/\$/, '').replace(/\[.*\]/, '')
    : draggingData.label
  
  const nodeConfig = {
    x: clientX - 60,
    y: clientY - 20,
    width: 150,
    height: 50,
    shape: 'rect',
    label: fieldName,
    data: {
      type: draggingData.nodeType,
      path: draggingData.path,
      nodeId: `${draggingData.nodeType}_${nodeCounter}`,
      fieldName: fieldName
    },
    // 添加连接桩配置
    ports: {
      groups: {
        top: {
          position: 'top',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
        right: {
          position: 'right',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
        bottom: {
          position: 'bottom',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
        left: {
          position: 'left',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
      },
      items: draggingData.nodeType === 'source' 
        ? [
            { id: 'port-right', group: 'right' }, // 源节点只有右侧输出
          ]
        : [
            { id: 'port-left', group: 'left' }, // 目标节点只有左侧输入
          ],
    },
    attrs: {
      body: {
        fill: draggingData.nodeType === 'source' ? '#e3f2fd' : '#f3e5f5',
        stroke: draggingData.nodeType === 'source' ? '#2196f3' : '#9c27b0',
        rx: 4,
        ry: 4,
      },
      text: {
        fill: draggingData.nodeType === 'source' ? '#1976d2' : '#7b1fa2',
        fontSize: 12,
      },
    },
  }
  
  graph.addNode(nodeConfig)
  draggingData = null
}

let currentEdge = null

const openNodeEditDialog = (node) => {
  currentNodeToEdit = node
  const nodeData = node.getData() || {}
  const currentLabel = node.attr('text/text') || ''
  
  currentNodeEdit.value = {
    fieldName: currentLabel,
    path: nodeData.path || currentLabel
  }
  
  nodeEditVisible.value = true
}

const saveNodeEdit = () => {
  if (!currentNodeToEdit) return
  
  // 更新节点标签
  currentNodeToEdit.attr('text/text', currentNodeEdit.value.fieldName)
  
  // 更新节点数据
  const nodeData = currentNodeToEdit.getData() || {}
  nodeData.path = currentNodeEdit.value.path
  nodeData.fieldName = currentNodeEdit.value.fieldName
  currentNodeToEdit.setData(nodeData)
  
  nodeEditVisible.value = false
  currentNodeToEdit = null
  ElMessage.success('字段已更新')
}

const openEdgeConfig = (edge) => {
  currentEdge = edge
  const sourceNode = graph.getCellById(edge.getSourceCellId())
  const targetNode = graph.getCellById(edge.getTargetCellId())
  
  if (sourceNode && targetNode) {
    const edgeData = edge.getData() || {}
    const sourceData = sourceNode.getData() || {}
    const targetData = targetNode.getData() || {}
    
    currentEdgeConfig.value = {
      sourcePath: sourceData.path || '',
      targetPath: targetData.path || '',
      mappingType: edgeData.mappingType || 'ONE_TO_ONE',
      transformType: edgeData.transformType || 'DIRECT',
      transformConfig: edgeData.transformConfig || {}
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
}

const onTransformTypeChange = () => {
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

const removeDictItem = (index) => {
  dictKeys.value.splice(index, 1)
  dictValues.value.splice(index, 1)
}

const saveEdgeConfig = () => {
  if (!currentEdge) return
  
  // 确保transformConfig存在
  if (!currentEdgeConfig.value.transformConfig) {
    currentEdgeConfig.value.transformConfig = {}
  }
  
  // 保存字典配置
  if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    const dict = {}
    dictKeys.value.forEach((key, index) => {
      if (key && dictValues.value[index]) {
        dict[key] = dictValues.value[index]
      }
    })
    currentEdgeConfig.value.transformConfig.dictionary = dict
  }
  
  // 更新当前边的数据
  currentEdge.setData({
    mappingType: currentEdgeConfig.value.mappingType,
    transformType: currentEdgeConfig.value.transformType,
    transformConfig: currentEdgeConfig.value.transformConfig
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
}

const exportConfig = () => {
  const config = exportMappingConfig()
  console.log('导出的配置:', config)
  
  // 复制到剪贴板或下载
  const jsonStr = JSON.stringify(config, null, 2)
  navigator.clipboard.writeText(jsonStr).then(() => {
    ElMessage.success('配置已复制到剪贴板')
  })
}

const exportMappingConfig = () => {
  const nodes = graph.getNodes()
  const edges = graph.getEdges()
  
  const rules = []
  
  edges.forEach(edge => {
    const sourceNode = graph.getCellById(edge.getSourceCellId())
    const targetNode = graph.getCellById(edge.getTargetCellId())
    
      if (sourceNode && targetNode) {
        const sourceData = sourceNode.getData()
        const targetData = targetNode.getData()
        const edgeData = edge.getData() || {}
        
        // 使用节点数据中的path，如果没有则使用JsonPath格式（源节点需要$前缀）
        let sourcePath = sourceData?.path || ''
        if (sourcePath && !sourcePath.startsWith('$') && sourceData?.type === 'source') {
          sourcePath = '$.' + sourcePath.replace(/^\./, '')
        }
        
        let targetPath = targetData?.path || ''
        // 目标路径不需要$前缀，保持原样
        
        const rule = {
          sourcePath: sourcePath,
          targetPath: targetPath,
          mappingType: edgeData.mappingType || 'ONE_TO_ONE',
          transformType: edgeData.transformType || 'DIRECT',
          transformConfig: edgeData.transformConfig || {}
        }
        
        rules.push(rule)
      }
  })
  
  return {
    sourceProtocol: 'JSON',
    targetProtocol: 'JSON',
    prettyPrint: true,
    rules: rules
  }
}

const testTransform = async () => {
  if (!sourceJson.value.trim()) {
    ElMessage.warning('请输入源数据')
    return
  }
  
  const config = exportMappingConfig()
  if (config.rules.length === 0) {
    ElMessage.warning('请先配置映射规则')
    return
  }
  
  try {
    const response = await transformV2Api.transform(sourceJson.value, config)
    
    if (response.data.success) {
      ElMessage.success('转换成功')
      // 显示转换结果
      console.log('转换结果:', response.data.transformedData)
      targetJson.value = response.data.transformedData
      // 重新解析目标树
      parseTargetTree()
    } else {
      ElMessage.error('转换失败: ' + response.data.errorMessage)
    }
  } catch (error) {
    ElMessage.error('转换失败: ' + error.message)
  }
}
</script>

<style scoped>
.canvas-editor {
  padding: 20px;
}

.editor-card {
  height: calc(100vh - 100px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 200px);
}

.source-panel,
.target-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
}

.canvas-panel {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.graph-container {
  width: 100%;
  height: 100%;
  background-color: #fafafa;
  position: relative;
}

.graph-container::before {
  content: '拖拽左侧或右侧的字段节点到此处';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #ccc;
  font-size: 14px;
  pointer-events: none;
  z-index: 0;
}

.graph-container > div {
  z-index: 1;
}

.data-tree {
  flex: 1;
  overflow: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 5px;
}

.dict-config {
  width: 100%;
}

.dict-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

h3 {
  margin: 0 0 10px 0;
}
</style>

