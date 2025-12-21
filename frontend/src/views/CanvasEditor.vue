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
                  @dblclick="handleTreeNodeDoubleClick(data, 'source')"
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
              placeholder="配置映射规则后，点击刷新预览查看转换结果"
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
        <el-form-item label="字段名">
          <el-input v-model="currentNodeEdit.fieldName" />
        </el-form-item>
        <el-form-item label="字段路径">
          <el-input v-model="currentNodeEdit.path" />
        </el-form-item>
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
        <el-form-item label="转换类型">
          <el-select v-model="currentEdgeConfig.transformType" @change="onTransformTypeChange">
            <el-option label="直接赋值" value="DIRECT" />
            <el-option label="函数映射" value="FUNCTION" />
            <el-option label="Groovy脚本" value="GROOVY" />
            <el-option label="字典映射" value="DICTIONARY" />
            <el-option label="固定值" value="FIXED" />
          </el-select>
        </el-form-item>
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
// 导入 Selection 插件
import { Selection } from '@antv/x6-plugin-selection'
import { ElMessage } from 'element-plus'
import { Document, Delete } from '@element-plus/icons-vue'
import { transformV2Api } from '../api'

const sourceJson = ref('')
const sourceTreeData = ref([])
const graphContainer = ref(null)
let graph = null

const previewResult = ref('')
const previewError = ref('')
const previewing = ref(false)
let keyDownHandler = null

const treeProps = { children: 'children', label: 'label' }
const edgeConfigVisible = ref(false)
const nodeEditVisible = ref(false)
let currentNodeToEdit = null
const currentNodeEdit = ref({ fieldName: '', path: '' })
const currentEdgeConfig = ref({ sourcePath: '', targetPath: '', mappingType: 'ONE_TO_ONE', transformType: 'DIRECT', transformConfig: {} })
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
      eventTypes: ['rightMouseDown'], // 改为右键平移，左键留给框选
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

  // --- 使用插件配置选择功能 ---
  graph.use(
      new Selection({
        enabled: true,
        multiple: true,
        rubberband: true, // 启用左键框选
        movable: true,
        showNodeSelectionBox: true, // 节点选中时显示外框
        showEdgeSelectionBox: true, // 边选中时显示外框
      })
  )

  // 监听双击打开配置
  graph.on('node:dblclick', ({ node }) => {
    if (node.getData()?.type === 'target') openNodeEditDialog(node)
  })

  graph.on('edge:dblclick', ({ edge }) => {
    openEdgeConfig(edge)
  })

  // 自动更新预览
  graph.on('edge:connected', () => nextTick(() => updatePreview()))
  graph.on('node:removed', () => nextTick(() => updatePreview()))
  graph.on('edge:removed', () => nextTick(() => updatePreview()))

  // 键盘删除逻辑（直接从 graph 获取选中项）
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

// --- 逻辑函数部分 ---

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

let draggingData = null
let nodeCounter = 0
const handleTreeDragStart = (e, data) => { draggingData = data }
const handleTreeDragEnd = () => { draggingData = null }

const handleCanvasDrop = (event) => {
  if (!graph || !draggingData) return
  const rect = graphContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const fieldName = draggingData.label
  const sId = `s_${++nodeCounter}`, tId = `t_${++nodeCounter}`

  // 1. 创建源节点
  graph.addNode({
    id: sId, x: x - 200, y: y - 25, width: 140, height: 40, label: fieldName,
    data: { type: 'source', path: draggingData.path },
    ports: {
      groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'right' }]
    },
    attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: fieldName } }
  })

  // 2. 创建目标节点
  graph.addNode({
    id: tId, x: x + 50, y: y - 25, width: 140, height: 40, label: fieldName,
    data: { type: 'target', path: fieldName },
    ports: {
      groups: { left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#9c27b0', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'left' }]
    },
    attrs: { body: { fill: '#f3e5f5', stroke: '#9c27b0', rx: 4 }, text: { text: fieldName } }
  })

  // 3. 创建初始连线
  graph.addEdge({
    source: { cell: sId, port: 'p1' },
    target: { cell: tId, port: 'p1' },
    attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
    data: { transformType: 'DIRECT', mappingType: 'ONE_TO_ONE' },
    labels: [{ attrs: { text: { text: 'DIRECT', fontSize: 10 } } }]
  })

  // 4. 重点：手动触发预览刷新
  nextTick(() => {
    updatePreview()
  })
}

// 对话框及预览逻辑保持原样，省略重复部分以减小篇幅...
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
  const data = edge.getData()
  currentEdgeConfig.value = { sourcePath: s.getData().path, targetPath: t.getData().path, transformType: data.transformType || 'DIRECT', transformConfig: data.transformConfig || {} }
  edgeConfigVisible.value = true
}

let currentEdge = null
const saveEdgeConfig = () => {
  currentEdge.setData(currentEdgeConfig.value)
  currentEdge.setLabels([{ attrs: { text: { text: currentEdgeConfig.value.transformType } } }])
  edgeConfigVisible.value = false
  updatePreview()
}

const updatePreview = async () => {
  if (!sourceJson.value) return
  const config = exportMappingConfig()
  if (!config.rules.length) return
  previewing.value = true
  try {
    const res = await transformV2Api.transform(sourceJson.value, config)
    previewResult.value = JSON.stringify(JSON.parse(res.data.transformedData), null, 2)
  } catch (e) { previewError.value = '转换失败' }
  finally { previewing.value = false }
}

const exportMappingConfig = () => {
  const rules = graph.getEdges().map(edge => {
    const s = graph.getCellById(edge.getSourceCellId()), t = graph.getCellById(edge.getTargetCellId())
    const data = edge.getData()
    return {
      sourcePath: s.getData().path.startsWith('$') ? s.getData().path : `$.${s.getData().path}`,
      targetPath: t.getData().path,
      transformType: data.transformType,
      transformConfig: data.transformConfig
    }
  })
  return { rules }
}

const exportConfig = () => {
  const config = exportMappingConfig()
  navigator.clipboard.writeText(JSON.stringify(config, null, 2))
  ElMessage.success('配置已复制')
}

const handleTreeNodeDoubleClick = (data) => {
  const mockEvent = { clientX: window.innerWidth / 2, clientY: window.innerHeight / 2, preventDefault: () => {} }
  draggingData = data
  handleCanvasDrop(mockEvent)
  draggingData = null
}
</script>

<style scoped>
.canvas-editor { padding: 20px; }
.editor-card { height: calc(100vh - 100px); }
.editor-container { display: flex; gap: 20px; height: calc(100vh - 200px); }
.source-panel { width: 300px; display: flex; flex-direction: column; }
.canvas-panel { flex: 1; border: 1px solid #ddd; border-radius: 4px; overflow: hidden; }
.graph-container { width: 100%; height: 100%; background-color: #fafafa; }
.preview-panel { width: 350px; display: flex; flex-direction: column; }
.data-tree { flex: 1; overflow: auto; border: 1px solid #ddd; padding: 10px; }

/* 必须添加：确保选中状态有视觉反馈 */
:deep(.x6-node-selected) rect {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}
:deep(.x6-edge-selected) path {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}
</style>