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
          <div class="tree-tip">拖拽字段到画布，自动创建映射</div>
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
        
        <!-- 右侧：预览结果 -->
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
            placeholder="配置映射规则后，点击刷新预览查看转换结果"
            style="font-family: 'Courier New', monospace;"
          />
          <div v-if="previewError" style="margin-top: 10px; color: #f56c6c; font-size: 12px;">
            {{ previewError }}
          </div>
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
import { ref, onMounted, nextTick, watch, watchEffect, onUnmounted } from 'vue'
import { Graph } from '@antv/x6'
import { ElMessage } from 'element-plus'
import { Document, Delete } from '@element-plus/icons-vue'
import { transformV2Api } from '../api'

const sourceJson = ref('')
const sourceTreeData = ref([])
const sourceTreeRef = ref(null)
const graphContainer = ref(null)
let graph = null
let selectedCellsSet = new Set() // 跟踪选中的单元格

const previewResult = ref('')
const previewError = ref('')
const previewing = ref(false)
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
  
  // 清空选中集合
  selectedCellsSet.clear()
  
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
    selecting: {
      enabled: true,
      rubberband: false, // 禁用框选，避免与拖拽冲突
      strict: false,
      showNodeSelectionBox: false,
      multiple: true, // 允许多选
      movable: true,
      selectCellOnMoved: false,
      selectNodeOnMoved: false,
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
        // 允许多条线连向同一个目标节点（支持多对一映射）
        if (sourceCell && targetCell && sourceCell !== targetCell) {
          const sourceData = sourceCell.getData()
          const targetData = targetCell.getData()
          return sourceData?.type === 'source' && targetData?.type === 'target'
        }
        return false
      },
    },
  })
  
  // 监听节点选中事件
  graph.on('node:selected', ({ node }) => {
    selectedCellsSet.add(node)
  })
  
  // 监听节点取消选中事件
  graph.on('node:unselected', ({ node }) => {
    selectedCellsSet.delete(node)
  })
  
  // 监听边选中事件
  graph.on('edge:selected', ({ edge }) => {
    selectedCellsSet.add(edge)
  })
  
  // 监听边取消选中事件
  graph.on('edge:unselected', ({ edge }) => {
    selectedCellsSet.delete(edge)
  })
  
  // 手动添加选中样式
  const addSelectedStyle = (cell) => {
    try {
      const view = graph.findViewByCell(cell)
      if (view && view.container) {
        view.container.classList.add('x6-cell-selected')
        // 检查是否是节点
        const isNode = cell.shape === 'rect' || cell.isNode?.() || (cell.getData && cell.getData().type)
        if (isNode) {
          view.container.classList.add('x6-node-selected')
          // 修改节点边框颜色来显示选中状态
          const attrs = cell.getAttrs()
          if (attrs && attrs.body) {
            cell.setAttrs({
              body: {
                ...attrs.body,
                stroke: '#ff4d4f',
                strokeWidth: 3
              }
            })
          }
        } else {
          view.container.classList.add('x6-edge-selected')
          // 修改边的颜色来显示选中状态
          const attrs = cell.getAttrs()
          if (attrs && attrs.line) {
            cell.setAttrs({
              line: {
                ...attrs.line,
                stroke: '#ff4d4f',
                strokeWidth: 3
              }
            })
          }
        }
      }
    } catch (e) {
      console.warn('添加选中样式失败:', e)
    }
  }
  
  // 手动移除选中样式
  const removeSelectedStyle = (cell) => {
    try {
      const view = graph.findViewByCell(cell)
      if (view && view.container) {
        view.container.classList.remove('x6-cell-selected', 'x6-node-selected', 'x6-edge-selected')
        // 恢复原始样式
        const isNode = cell.shape === 'rect' || cell.isNode?.() || (cell.getData && cell.getData().type)
        if (isNode) {
          const data = cell.getData()
          const isSource = data?.type === 'source'
          const attrs = cell.getAttrs()
          if (attrs && attrs.body) {
            cell.setAttrs({
              body: {
                ...attrs.body,
                stroke: isSource ? '#2196f3' : '#9c27b0',
                strokeWidth: 1
              }
            })
          }
        } else {
          const attrs = cell.getAttrs()
          if (attrs && attrs.line) {
            cell.setAttrs({
              line: {
                ...attrs.line,
                stroke: '#8f8f8f',
                strokeWidth: 2
              }
            })
          }
        }
      }
    } catch (e) {
      console.warn('移除选中样式失败:', e)
    }
  }
  
  // 监听节点点击事件（手动选中节点）
  graph.on('node:click', ({ node, e }) => {
    e.stopPropagation()
    // 切换选中状态
    if (selectedCellsSet.has(node)) {
      // 如果已选中，取消选中
      selectedCellsSet.delete(node)
      removeSelectedStyle(node)
      console.log('节点取消选中:', node.id)
    } else {
      // 如果未选中，选中它
      selectedCellsSet.add(node)
      addSelectedStyle(node)
      console.log('节点选中:', node.id)
    }
  })
  
  // 监听连线单击事件（只选中边，不打开配置）
  graph.on('edge:click', ({ edge, e }) => {
    e.stopPropagation()
    // 切换选中状态
    if (selectedCellsSet.has(edge)) {
      // 如果已选中，取消选中
      selectedCellsSet.delete(edge)
      removeSelectedStyle(edge)
      console.log('边取消选中:', edge.id)
    } else {
      // 如果未选中，选中它
      selectedCellsSet.add(edge)
      addSelectedStyle(edge)
      console.log('边选中:', edge.id)
    }
  })
  
  // 监听连线双击事件（打开配置窗口）
  graph.on('edge:dblclick', ({ edge, e }) => {
    e.stopPropagation()
    // 双击时选中并打开配置
    if (!selectedCellsSet.has(edge)) {
      selectedCellsSet.add(edge)
      addSelectedStyle(edge)
    }
    openEdgeConfig(edge)
  })
  
  // 监听画布空白区域点击（取消选中）
  graph.on('blank:click', () => {
    // 取消所有选中
    const cellsToUnselect = Array.from(selectedCellsSet)
    cellsToUnselect.forEach(cell => {
      removeSelectedStyle(cell)
    })
    selectedCellsSet.clear()
    console.log('空白区域点击，清空选中')
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
    
    // 连线创建后自动更新预览
    nextTick(() => {
      updatePreview()
    })
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
    // 从选中集合中移除
    selectedCellsSet.delete(node)
    
    // 节点删除时，自动删除相关的连线
    const edges = graph.getEdges()
    edges.forEach(edge => {
      const sourceId = edge.getSourceCellId()
      const targetId = edge.getTargetCellId()
      if (sourceId === node.id || targetId === node.id) {
        selectedCellsSet.delete(edge) // 同时从选中集合中移除连线
        graph.removeEdge(edge.id)
      }
    })
    
    // 节点删除后自动更新预览
    nextTick(() => {
      updatePreview()
    })
  })
  
  // 监听边的删除
  graph.on('edge:removed', ({ edge }) => {
    // 从选中集合中移除
    selectedCellsSet.delete(edge)
    
    nextTick(() => {
      updatePreview()
    })
  })
  
  // 启用键盘删除（使用window事件监听）
  keyDownHandler = (e) => {
    // 只处理 Delete 和 Backspace 键
    if (e.key !== 'Delete' && e.key !== 'Backspace') {
      return
    }
    
    // 检查是否有选中的单元格
    if (!graph) {
      return
    }
    
    // 获取选中的单元格 - 直接使用我们手动维护的集合
    const selectedCells = Array.from(selectedCellsSet).filter(cell => {
      // 确保单元格仍然存在于图中
      return cell && graph.getCellById(cell.id)
    })
    
    console.log('准备删除，selectedCellsSet大小:', selectedCellsSet.size, '筛选后:', selectedCells.length)
    
    if (selectedCells.length === 0) {
      console.log('没有选中的单元格可以删除')
      return
    }
    
    console.log('找到', selectedCells.length, '个选中的单元格:', selectedCells.map(c => c.id))
    
    // 检查焦点是否在输入框中
    const activeElement = document.activeElement
    const isInput = activeElement && (
      activeElement.tagName === 'INPUT' || 
      activeElement.tagName === 'TEXTAREA' ||
      activeElement.isContentEditable ||
      activeElement.closest('.el-input') ||
      activeElement.closest('.el-textarea') ||
      activeElement.closest('.el-input__inner') ||
      activeElement.closest('.el-textarea__inner')
    )
    
    // 如果焦点在输入框中，不执行删除（让输入框自己处理）
    if (isInput) {
      return
    }
    
    // 执行删除
    e.preventDefault()
    e.stopPropagation()
    e.stopImmediatePropagation()
    
    try {
      console.log('执行删除操作，删除', selectedCells.length, '个单元格')
      graph.removeCells(selectedCells)
      
      // 从选中集合中移除已删除的单元格
      selectedCells.forEach(cell => {
        selectedCellsSet.delete(cell)
      })
      
      // 删除后更新预览
      nextTick(() => {
        updatePreview()
      })
    } catch (error) {
      console.error('删除节点/连线失败:', error, error.stack)
    }
  }
  
  // 使用window监听键盘事件（捕获阶段，确保优先处理）
  window.addEventListener('keydown', keyDownHandler, true)
  
  // 确保画布容器可以接收焦点
  if (graphContainer.value) {
    graphContainer.value.setAttribute('tabindex', '0')
    graphContainer.value.style.outline = 'none' // 移除焦点时的边框
  }
}

const parseSourceTree = () => {
  try {
    if (!sourceJson.value.trim()) {
      sourceTreeData.value = []
      return
    }
    const data = JSON.parse(sourceJson.value)
    sourceTreeData.value = [convertToTreeData(data, 'source', '$')]
    
    // 默认展开所有节点（使用双重 nextTick 确保 DOM 完全更新）
    nextTick(() => {
      nextTick(() => {
        if (sourceTreeRef.value && sourceTreeData.value.length > 0) {
          try {
            // 获取所有节点key
            const allKeys = []
            const getAllKeys = (nodes) => {
              nodes.forEach(node => {
                if (node.path) {
                  allKeys.push(node.path)
                }
                if (node.children && node.children.length > 0) {
                  getAllKeys(node.children)
                }
              })
            }
            getAllKeys(sourceTreeData.value)
            
            // 调用 setExpandedKeys 方法展开所有节点
            if (allKeys.length > 0) {
              sourceTreeRef.value.setExpandedKeys(allKeys)
              console.log('已展开', allKeys.length, '个节点')
            }
          } catch (e) {
            console.error('展开节点失败:', e)
          }
        }
      })
    })
  } catch (e) {
    // JSON解析失败时显示错误提示
    sourceTreeData.value = []
    ElMessage.error('JSON格式错误: ' + (e.message || '请检查JSON格式是否正确'))
  }
}

// 更新预览结果
const updatePreview = async () => {
  if (!sourceJson.value.trim()) {
    previewError.value = '请先输入源数据'
    previewResult.value = ''
    return
  }
  
  const config = exportMappingConfig()
  if (config.rules.length === 0) {
    previewError.value = '请先配置映射规则（拖拽字段到画布并连线）'
    previewResult.value = ''
    return
  }
  
  previewing.value = true
  previewError.value = ''
  
  try {
    const response = await transformV2Api.transform(sourceJson.value, config)
    
    if (response.data && response.data.success) {
      // 如果返回的是JSON字符串，格式化显示
      try {
        const jsonData = JSON.parse(response.data.transformedData)
        previewResult.value = JSON.stringify(jsonData, null, 2)
      } catch (e) {
        previewResult.value = response.data.transformedData
      }
      previewError.value = ''
    } else {
      previewError.value = '转换失败: ' + (response.data?.errorMessage || '未知错误')
      previewResult.value = ''
    }
  } catch (error) {
    previewError.value = '转换失败: ' + (error.response?.data?.errorMessage || error.message)
    previewResult.value = ''
  } finally {
    previewing.value = false
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
  
  // 只处理源节点的拖拽
  if (!graph || !draggingData || draggingData.nodeType !== 'source') return
  
  const rect = graphContainer.value.getBoundingClientRect()
  const clientX = event.clientX - rect.left
  const clientY = event.clientY - rect.top
  
  // 提取字段名（从路径中提取最后一个字段）
  let fieldName = draggingData.label
  if (draggingData.path) {
    const pathParts = draggingData.path.split('.')
    fieldName = pathParts[pathParts.length - 1].replace(/\$/, '').replace(/\[.*\]/, '')
  }
  
  nodeCounter++
  
  // 创建源节点（蓝色）- 左侧
  const sourceNodeId = `source_${nodeCounter}`
  const sourceNodeConfig = {
    x: clientX - 180,
    y: clientY - 25,
    width: 150,
    height: 50,
    shape: 'rect',
    label: fieldName,
    id: sourceNodeId,
    selectable: true, // 确保可选中
    movable: true, // 确保可移动
    data: {
      type: 'source',
      path: draggingData.path,
      nodeId: sourceNodeId,
      fieldName: fieldName
    },
    ports: {
      groups: {
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
      },
      items: [
        { id: 'port-right', group: 'right' },
      ],
    },
    attrs: {
      body: {
        fill: '#e3f2fd',
        stroke: '#2196f3',
        rx: 4,
        ry: 4,
      },
      text: {
        fill: '#1976d2',
        fontSize: 12,
      },
    },
  }
  
  // 创建目标节点（紫色）- 右侧
  nodeCounter++
  const targetNodeId = `target_${nodeCounter}`
  const targetNodeConfig = {
    x: clientX + 30,
    y: clientY - 25,
    width: 150,
    height: 50,
    shape: 'rect',
    label: fieldName,
    id: targetNodeId,
    selectable: true, // 确保可选中
    movable: true, // 确保可移动
    data: {
      type: 'target',
      path: fieldName, // 默认路径就是字段名
      nodeId: targetNodeId,
      fieldName: fieldName
    },
    ports: {
      groups: {
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
      items: [
        { id: 'port-left', group: 'left' },
      ],
    },
    attrs: {
      body: {
        fill: '#f3e5f5',
        stroke: '#9c27b0',
        rx: 4,
        ry: 4,
      },
      text: {
        fill: '#7b1fa2',
        fontSize: 12,
      },
    },
  }
  
  // 添加节点到画布
  graph.addNode(sourceNodeConfig)
  graph.addNode(targetNodeConfig)
  
  // 自动创建连线（从源节点到目标节点）
  graph.addEdge({
    source: { cell: sourceNodeId, port: 'port-right' },
    target: { cell: targetNodeId, port: 'port-left' },
    selectable: true, // 确保连线可选中
    attrs: {
      line: {
        stroke: '#8f8f8f',
        strokeWidth: 2,
        targetMarker: {
          name: 'classic',
          size: 7,
        },
      },
    },
    data: {
      mappingType: 'ONE_TO_ONE',
      transformType: 'DIRECT',
      transformConfig: {}
    },
    labels: [{
      attrs: {
        text: {
          text: 'DIRECT',
          fill: '#666',
          fontSize: 10,
        }
      }
    }]
  })
  
  draggingData = null
  
  // 自动更新预览
  nextTick(() => {
    updatePreview()
  })
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
  
  // 字段更新后自动更新预览
  nextTick(() => {
    updatePreview()
  })
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
  
  // 配置保存后自动更新预览
  nextTick(() => {
    updatePreview()
  })
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
  
  // 按目标节点分组，处理多对一映射
  const targetToEdgesMap = new Map()
  
  edges.forEach(edge => {
    const targetNode = graph.getCellById(edge.getTargetCellId())
    if (targetNode) {
      const targetId = targetNode.id
      if (!targetToEdgesMap.has(targetId)) {
        targetToEdgesMap.set(targetId, [])
      }
      targetToEdgesMap.get(targetId).push(edge)
    }
  })
  
  const rules = []
  
  // 遍历每个目标节点，生成规则
  targetToEdgesMap.forEach((edgesList, targetId) => {
    const targetNode = graph.getCellById(targetId)
    if (!targetNode) return
    
    const targetData = targetNode.getData()
    const targetPath = targetData?.path || ''
    
    if (edgesList.length === 1) {
      // 一对一映射
      const edge = edgesList[0]
      const sourceNode = graph.getCellById(edge.getSourceCellId())
      if (!sourceNode) return
      
      const sourceData = sourceNode.getData()
      const edgeData = edge.getData() || {}
      
      let sourcePath = sourceData?.path || ''
      if (sourcePath && !sourcePath.startsWith('$') && sourceData?.type === 'source') {
        sourcePath = '$.' + sourcePath.replace(/^\./, '')
      }
      
      const rule = {
        sourcePath: sourcePath,
        targetPath: targetPath,
        mappingType: edgeData.mappingType || 'ONE_TO_ONE',
        transformType: edgeData.transformType || 'DIRECT',
        transformConfig: edgeData.transformConfig || {}
      }
      rules.push(rule)
    } else {
      // 多对一映射：多条边指向同一个目标节点
      const sourcePaths = []
      const firstEdgeData = edgesList[0].getData() || {}
      
      edgesList.forEach(edge => {
        const sourceNode = graph.getCellById(edge.getSourceCellId())
        if (sourceNode) {
          const sourceData = sourceNode.getData()
          let sourcePath = sourceData?.path || ''
          if (sourcePath && !sourcePath.startsWith('$') && sourceData?.type === 'source') {
            sourcePath = '$.' + sourcePath.replace(/^\./, '')
          }
          if (sourcePath) {
            sourcePaths.push(sourcePath)
          }
        }
      })
      
      if (sourcePaths.length > 0) {
        const rule = {
          sourcePath: sourcePaths[0], // 主源路径
          additionalSources: sourcePaths.slice(1), // 额外的源路径
          targetPath: targetPath,
          mappingType: 'MANY_TO_ONE',
          transformType: firstEdgeData.transformType || 'GROOVY',
          transformConfig: firstEdgeData.transformConfig || {
            // 默认Groovy脚本：拼接所有输入值
            groovyScript: `def parts = input as List; return parts.collect { it?.toString() ?: '' }.join(' ')`
          }
        }
        rules.push(rule)
      }
    }
  })
  
  return {
    sourceProtocol: 'JSON',
    targetProtocol: 'JSON',
    prettyPrint: true,
    rules: rules
  }
}

// testTransform 已改为 updatePreview
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

.source-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
}

.preview-panel {
  width: 350px;
  display: flex;
  flex-direction: column;
}

.tree-tip {
  font-size: 12px;
  color: #666;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 10px;
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
  content: '拖拽左侧字段到此处，将自动创建源节点和目标节点';
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

