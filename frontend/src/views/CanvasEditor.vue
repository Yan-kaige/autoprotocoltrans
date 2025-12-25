<template>
  <div class="canvas-editor">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <span>报文映射画布编辑器</span>
          <div>
            <el-button @click="openSaveConfigDialog">{{ currentConfigId ? '修改配置' : '保存配置' }}</el-button>
          </div>
        </div>
      </template>

      <div class="editor-container">
        <div class="source-panel">
          <h3>源数据</h3>
          <div style="margin-bottom: 10px;">
            <el-select v-model="sourceProtocol" style="width: 100%;" @change="onSourceProtocolChange">
              <el-option label="JSON" value="JSON" />
              <el-option label="XML" value="XML" />
            </el-select>
          </div>
          <el-input
              v-model="sourceJson"
              type="textarea"
              :rows="10"
              :placeholder="sourceProtocol === 'XML' ? '请输入源XML数据' : '请输入源JSON数据'"
              @input="parseSourceTree"
          />
          <el-divider />
          <div class="tree-tip">拖拽或双击字段到画布（也可在画布上直接点击"添加节点"按钮）</div>
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
          <div class="canvas-toolbar">
            <el-button type="primary" size="small" @click="addNodePair">
              <el-icon><Plus /></el-icon>
              添加节点
            </el-button>
          </div>
          <div ref="graphContainer" class="graph-container"></div>
        </div>

        <div class="preview-panel">
          <h3>转换预览</h3>
          <div style="margin-bottom: 10px;">
            <el-select v-model="targetProtocol" style="width: 100%; margin-bottom: 10px;">
              <el-option label="JSON" value="JSON" />
              <el-option label="XML" value="XML" />
            </el-select>
            <el-input
                v-if="targetProtocol === 'XML'"
                v-model="xmlRootElementName"
                placeholder="请输入XML根元素名称（必填）"
                style="width: 100%; margin-bottom: 10px;"
            />
            <el-checkbox
                v-if="targetProtocol === 'XML'"
                v-model="includeXmlDeclaration"
                style="margin-top: 5px;"
            >
                包含XML声明
            </el-checkbox>
            <div v-if="targetProtocol === 'XML'" style="font-size: 12px; color: #999; margin-top: 5px;">
              （&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;）
            </div>
          </div>
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

    <el-dialog v-model="nodeEditVisible" :title="currentNodeEditType === 'source' ? '编辑源字段' : '编辑目标字段'" width="500px">
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
          <el-select v-model="currentEdgeConfig.mappingType" @change="onMappingTypeChange">
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
        
        <!-- 一对多映射的子映射配置 -->
        <el-form-item v-if="currentEdgeConfig.mappingType === 'ONE_TO_MANY'" label="子映射配置">
          <div class="sub-mapping-config">
            <div v-for="(sub, index) in subMappings" :key="index" class="sub-mapping-item">
              <el-input 
                v-model="sub.sourcePath" 
                :placeholder="currentEdgeConfig.transformType === 'GROOVY' ? 'Map的key（如 province）或留空使用索引' : '源路径（相对于源对象，如 province）'" 
                style="width: 45%" 
              />
              <span style="margin: 0 10px">-></span>
              <el-input v-model="sub.targetPath" placeholder="目标路径（如 address.province）" style="width: 45%" />
              <el-button link type="danger" @click="removeSubMapping(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="addSubMapping" style="margin-top: 10px">添加子映射</el-button>
            <div style="font-size: 12px; color: #999; margin-top: 5px;">
              <div v-if="currentEdgeConfig.transformType === 'GROOVY'">
                <div><strong>字符串拆分模式：</strong>使用Groovy脚本将字符串拆分成Map，然后映射到多个目标字段</div>
                <div><strong>Groovy脚本示例（拆分"省-市-县"）：</strong></div>
                <div style="background: #f5f5f5; padding: 8px; border-radius: 4px; margin: 5px 0;">
                  <code>def parts = input?.toString()?.split('-') ?: []; return [province: parts[0] ?: '', city: parts[1] ?: '', district: parts[2] ?: '']</code>
                </div>
                <div><strong>子映射配置：</strong></div>
                <div>• 源路径填写 <code>province</code> → 目标路径填写 <code>address.province</code></div>
                <div>• 源路径填写 <code>city</code> → 目标路径填写 <code>address.city</code></div>
                <div>• 源路径填写 <code>district</code> → 目标路径填写 <code>address.district</code></div>
              </div>
              <div v-else>
                <div><strong>对象拆分模式：</strong>将源对象中的多个子字段映射到目标的不同路径</div>
                <div><strong>源路径填写：</strong>相对于源对象的路径，例如如果源路径是 <code>$.user.address</code>，子映射源路径填写 <code>province</code> 即可（不需要 <code>$.</code> 前缀）</div>
                <div>例如：源对象 <code>{"province": "北京", "city": "北京市"}</code>，子映射源路径填写 <code>province</code>，目标路径填写 <code>address.province</code></div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdgeConfig">确定</el-button>
      </template>
    </el-dialog>

    <!-- 保存/修改配置对话框 -->
    <el-dialog v-model="saveConfigVisible" :title="currentConfigId ? '修改配置' : '保存配置'" width="500px">
      <el-form :model="saveConfigForm" label-width="100px">
        <el-form-item label="配置名称" required>
          <el-input v-model="saveConfigForm.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="配置描述">
          <el-input
            v-model="saveConfigForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">{{ currentConfigId ? '修改' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { Graph } from '@antv/x6'
import { Selection } from '@antv/x6-plugin-selection'
import { ElMessage } from 'element-plus'
import { Document, Delete, Plus } from '@element-plus/icons-vue'
import { transformV2Api, configApi } from '../api'
import { useRoute, useRouter } from 'vue-router'

// --- 状态变量 ---
const sourceJson = ref('')
const sourceTreeData = ref([])
const sourceProtocol = ref('JSON') // 源协议类型：JSON 或 XML
const targetProtocol = ref('JSON') // 目标协议类型：JSON 或 XML
const xmlRootElementName = ref('') // XML根元素名称
const includeXmlDeclaration = ref(false) // 是否包含XML声明
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
const currentNodeEditType = ref('') // 'source' 或 'target'
const currentEdgeConfig = ref({ 
  sourcePath: '', 
  targetPath: '', 
  mappingType: 'ONE_TO_ONE',
  transformType: 'DIRECT', 
  transformConfig: {} 
})
const dictKeys = ref([])
const dictValues = ref([])
const subMappings = ref([]) // 一对多映射的子映射列表

// 保存配置相关
const saveConfigVisible = ref(false)
const saving = ref(false)
const saveConfigForm = ref({
  name: '',
  description: ''
})

// 路由相关
const route = useRoute()
const router = useRouter()
const currentConfigId = ref(null) // 当前编辑的配置ID
const currentConfigEntity = ref(null) // 当前编辑的配置实体（包含名称和描述）

onMounted(() => {
  nextTick(() => {
    initGraph()
    // 检查URL参数，如果是编辑模式则加载配置
    const configId = route.query.configId
    if (configId) {
      currentConfigId.value = configId
      loadConfigToCanvas(Number(configId))
    }
  })
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
    // 允许编辑源节点和目标节点
    const nodeType = node.getData()?.type
    if (nodeType === 'source' || nodeType === 'target') {
      openNodeEditDialog(node)
    }
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
    
    let data
    if (sourceProtocol.value === 'XML') {
      // XML格式：解析XML为对象
      data = parseXmlToObject(sourceJson.value)
    } else {
      // JSON格式：解析JSON
      data = JSON.parse(sourceJson.value)
    }
    
    // 转换为树结构显示
    sourceTreeData.value = [convertToTreeData(data, 'source', '$')]
    
    // 调试：输出解析后的JSON结构
    if (sourceProtocol.value === 'XML') {
      console.log('XML解析后的结构:', JSON.stringify(data, null, 2))
    }
  } catch (e) { 
    console.error('解析失败:', e)
    sourceTreeData.value = [] 
  }
}

// XML转对象（尽量与Jackson XmlMapper的结构保持一致）
const parseXmlToObject = (xmlString) => {
  try {
    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml')
    
    // 检查是否有解析错误
    const parseError = xmlDoc.querySelector('parsererror')
    if (parseError) {
      throw new Error('XML解析错误: ' + parseError.textContent)
    }
    
    // 递归解析XML节点
    const parseNode = (node) => {
      // 收集所有子元素节点
      const elementChildren = []
      let textContent = ''
      
      for (let i = 0; i < node.childNodes.length; i++) {
        const child = node.childNodes[i]
        if (child.nodeType === 3) { // 文本节点
          const text = child.textContent?.trim()
          if (text) {
            textContent += (textContent ? ' ' : '') + text
          }
        } else if (child.nodeType === 1) { // 元素节点
          elementChildren.push(child)
        }
      }
      
      // 如果有子元素，解析为对象
      if (elementChildren.length > 0) {
        const result = {}
        
        // 处理属性（Jackson通常不使用@前缀，但为了兼容性保留）
        if (node.attributes && node.attributes.length > 0) {
          for (let i = 0; i < node.attributes.length; i++) {
            const attr = node.attributes[i]
            result['@' + attr.name] = attr.value
          }
        }
        
        // 如果有文本内容且没有子元素，添加文本
        if (textContent && elementChildren.length === 0) {
          // 如果只有文本，Jackson通常直接返回文本值
          return textContent
        }
        
        // 解析子元素
        const elementMap = {}
        elementChildren.forEach(child => {
          const childName = child.nodeName
          const childValue = parseNode(child)
          
          // 处理同名节点（转为数组）
          if (elementMap[childName] !== undefined) {
            if (!Array.isArray(elementMap[childName])) {
              elementMap[childName] = [elementMap[childName]]
            }
            elementMap[childName].push(childValue)
          } else {
            elementMap[childName] = childValue
          }
        })
        
        // 合并属性、文本和子元素
        Object.assign(result, elementMap)
        
        // 如果有文本内容，添加到结果中（Jackson通常不使用#text）
        // 但为了前端显示，我们可以在某些情况下包含文本
        if (textContent && Object.keys(elementMap).length > 0) {
          // 如果既有文本又有子元素，可能需要特殊处理
          // 这里简化处理，只显示子元素
        }
        
        // 如果结果为空，返回空字符串
        if (Object.keys(result).length === 0) {
          return textContent || ''
        }
        
        return result
      } else {
        // 没有子元素，直接返回文本内容
        return textContent || ''
      }
    }
    
    // 从根元素开始解析
    const rootElement = xmlDoc.documentElement
    if (!rootElement) {
      throw new Error('XML格式错误：找不到根元素')
    }
    
    // Jackson XmlMapper解析XML时，会将根元素的内容直接解析为Map
    // 而不是包装在根元素名中，所以直接返回根元素的内容
    const rootValue = parseNode(rootElement)
    
    // 如果根元素的内容是一个对象，直接返回
    // 这与Jackson XmlMapper的行为一致
    if (typeof rootValue === 'object' && rootValue !== null) {
      return rootValue
    }
    
    // 如果根元素只有文本内容，包装一下
    const rootName = rootElement.nodeName
    const rootObj = {}
    rootObj[rootName] = rootValue
    return rootObj
  } catch (e) {
    throw new Error('XML解析失败: ' + e.message)
  }
}

// 源协议类型变化时的处理
const onSourceProtocolChange = () => {
  // 切换协议类型时，清空树数据并重新解析
  parseSourceTree()
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
/**
 * 创建节点对（源节点和目标节点）
 * @param {number} x - 中心X坐标
 * @param {number} y - 中心Y坐标
 * @param {string} fieldName - 字段名称
 * @param {string} sourcePath - 源路径（可选，如果为空则使用fieldName）
 */
const createNodePair = (x, y, fieldName = 'newField', sourcePath = null) => {
  if (!graph) return
  
  // 如果没有提供fieldName，使用默认值
  if (!fieldName || fieldName.trim() === '') {
    fieldName = `field_${nodeCounter + 1}`
  }
  
  // 如果没有提供sourcePath，使用fieldName作为路径
  if (!sourcePath) {
    sourcePath = fieldName
  }
  
  const sId = `s_${++nodeCounter}`
  const tId = `t_${++nodeCounter}`

  // 1. 源节点
  graph.addNode({
    id: sId, x: x - 200, y: y - 25, width: 140, height: 40, label: fieldName,
    data: { type: 'source', path: sourcePath },
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

/**
 * 画布放置逻辑：处理手动拖拽和双击生成的坐标
 */
const handleCanvasDrop = (event) => {
  if (!graph || !draggingData) return
  const rect = graphContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const fieldName = draggingData.label
  createNodePair(x, y, fieldName, draggingData.path)
}

/**
 * 手动添加节点对（点击按钮时调用）
 */
const addNodePair = () => {
  if (!graph || !graphContainer.value) return
  
  // 智能排版算法：每 10 个换一列
  const ROW_HEIGHT = 60
  const COLUMN_WIDTH = 450
  const row = autoLayoutCount % 10
  const col = Math.floor(autoLayoutCount / 10)
  
  // 基础起始点：x=250 (留出源节点空间), y=50
  // 这里使用目标节点的X坐标作为中心点（源节点在左侧200px，目标节点在右侧50px）
  const targetX = 250 + (col * COLUMN_WIDTH)
  const targetY = 50 + (row * ROW_HEIGHT)
  
  // 使用默认字段名创建节点对
  createNodePair(targetX, targetY, `field_${nodeCounter + 1}`, null)
  
  // 计数器加1，确保下次不重叠
  autoLayoutCount++
  
  ElMessage.success('已添加新节点，可以双击节点进行编辑')
}

// --- 配置与转换逻辑 ---
const openNodeEditDialog = (node) => {
  currentNodeToEdit = node
  const data = node.getData()
  currentNodeEditType.value = data.type || '' // 保存节点类型
  currentNodeEdit.value = { 
    fieldName: node.attr('text/text') || '', 
    path: data.path || '' 
  }
  nodeEditVisible.value = true
}

const saveNodeEdit = () => {
  if (!currentNodeToEdit) return
  
  // 更新节点文本标签
  currentNodeToEdit.attr('text/text', currentNodeEdit.value.fieldName)
  
  // 更新节点数据（包括路径）
  const currentData = currentNodeToEdit.getData()
  currentNodeToEdit.setData({ 
    ...currentData, 
    path: currentNodeEdit.value.path || currentNodeEdit.value.fieldName
  })
  
  nodeEditVisible.value = false
  nextTick(() => updatePreview())
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
  
  // 初始化一对多映射的子映射配置
  if (currentEdgeConfig.value.mappingType === 'ONE_TO_MANY') {
    const subMaps = currentEdgeConfig.value.transformConfig?.subMappings || []
    if (Array.isArray(subMaps)) {
      subMappings.value = subMaps.map(item => ({
        sourcePath: item.sourcePath || '',
        targetPath: item.targetPath || ''
      }))
      if (subMappings.value.length === 0) {
        subMappings.value = [{ sourcePath: '', targetPath: '' }]
      }
    } else {
      subMappings.value = [{ sourcePath: '', targetPath: '' }]
    }
  } else {
    subMappings.value = []
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

const onMappingTypeChange = () => {
  // 切换映射类型时，初始化相应的配置
  if (currentEdgeConfig.value.mappingType === 'ONE_TO_MANY') {
    // 一对多映射：初始化子映射列表
    if (subMappings.value.length === 0) {
      subMappings.value = [{ sourcePath: '', targetPath: '' }]
    }
  } else {
    // 其他映射类型：清空子映射
    subMappings.value = []
  }
}

// 一对多映射的子映射管理
const addSubMapping = () => {
  subMappings.value.push({ sourcePath: '', targetPath: '' })
}

const removeSubMapping = (index) => {
  subMappings.value.splice(index, 1)
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
  
  // 一对多映射：保存子映射配置（可以与其他转换类型组合使用，如GROOVY+ONE_TO_MANY）
  if (currentEdgeConfig.value.mappingType === 'ONE_TO_MANY') {
    // 一对多映射：保存子映射配置
    // 子映射的源路径是相对于源对象的路径，不需要$前缀
    const subMaps = subMappings.value
      .filter(item => item.sourcePath && item.targetPath)
      .map(item => ({
        sourcePath: item.sourcePath.replace(/^\$\./, ''), // 移除$前缀，使用相对路径
        targetPath: item.targetPath
      }))
    if (subMaps.length > 0) {
      config.subMappings = subMaps
    }
  }
  
  // 一对多映射：保存子映射配置（可以与其他转换类型组合使用，如GROOVY+ONE_TO_MANY）
  if (currentEdgeConfig.value.mappingType === 'ONE_TO_MANY') {
    // 一对多映射：保存子映射配置
    // 子映射的源路径是相对于源对象的路径，不需要$前缀
    const subMaps = subMappings.value
      .filter(item => item.sourcePath && item.targetPath)
      .map(item => ({
        sourcePath: item.sourcePath.replace(/^\$\./, ''), // 移除$前缀，使用相对路径
        targetPath: item.targetPath
      }))
    if (subMaps.length > 0) {
      config.subMappings = subMaps
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
  
  // 如果目标协议是XML，检查是否输入了根元素名称
  if (targetProtocol.value === 'XML' && !xmlRootElementName.value?.trim()) {
    previewError.value = '目标协议为XML时，请先输入XML根元素名称'
    return
  }
  
  const config = exportMappingConfig()
  if (!config.rules.length) return
  previewing.value = true
  try {
    const res = await transformV2Api.transform(sourceJson.value, config)
    // 根据目标协议类型决定是否格式化
    const transformedData = res.data.transformedData
    if (targetProtocol.value === 'JSON') {
      // JSON格式：尝试解析并格式化
      try {
        previewResult.value = JSON.stringify(JSON.parse(transformedData), null, 2)
      } catch (e) {
        previewResult.value = transformedData
      }
    } else {
      // XML格式：直接显示
      previewResult.value = transformedData
    }
    previewError.value = ''
  } catch (e) {
    const errorMsg = e.response?.data?.errorMessage || e.message || '未知错误'
    previewError.value = '转换失败: ' + errorMsg
    console.error('转换失败详情:', e.response?.data || e)
  } finally { previewing.value = false }
}

const exportMappingConfig = () => {
  const edges = graph.getEdges()
  const nodes = graph.getNodes()
  
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
      if (sourcePath && !sourcePath.startsWith('$')) {
        sourcePath = `$.${sourcePath.replace(/^\./, '')}`
      }
      
      const rule = {
        sourcePath: sourcePath,
        targetPath: targetPath,
        mappingType: edgeData.mappingType || 'ONE_TO_ONE',
        transformType: edgeData.transformType || 'DIRECT',
        transformConfig: edgeData.transformConfig ? JSON.parse(JSON.stringify(edgeData.transformConfig)) : {}
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
          if (sourcePath && !sourcePath.startsWith('$')) {
            sourcePath = `$.${sourcePath.replace(/^\./, '')}`
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
  
  const config = {
    sourceProtocol: sourceProtocol.value,
    targetProtocol: targetProtocol.value,
    prettyPrint: true,
    rules: rules
  }
  
  // 如果目标协议是XML，添加根元素名称和XML声明选项
  if (targetProtocol.value === 'XML') {
    if (xmlRootElementName.value) {
      config.xmlRootElementName = xmlRootElementName.value.trim()
    }
    config.includeXmlDeclaration = includeXmlDeclaration.value
  }
  
  return config
}

// 打开保存配置对话框
const openSaveConfigDialog = () => {
  const config = exportMappingConfig()
  if (!config.rules || config.rules.length === 0) {
    ElMessage.warning('请先配置映射规则')
    return
  }
  
  // 如果是编辑模式，填充原有的名称和描述
  if (currentConfigId.value && currentConfigEntity.value) {
    saveConfigForm.value.name = currentConfigEntity.value.name || ''
    saveConfigForm.value.description = currentConfigEntity.value.description || ''
  } else {
    // 新建模式，清空表单
    saveConfigForm.value.name = ''
    saveConfigForm.value.description = ''
  }
  
  saveConfigVisible.value = true
}

// 保存配置
const saveConfig = async () => {
  if (!saveConfigForm.value.name?.trim()) {
    ElMessage.warning('请输入配置名称')
    return
  }
  
  saving.value = true
  try {
    const config = exportMappingConfig()
    const res = await configApi.saveConfig(
      currentConfigId.value || null, // 如果是编辑模式，传递配置ID
      saveConfigForm.value.name.trim(),
      saveConfigForm.value.description?.trim() || '',
      config
    )
    
    if (res.data.success) {
      ElMessage.success(currentConfigId.value ? '配置修改成功' : '配置保存成功')
      saveConfigVisible.value = false
      currentConfigId.value = res.data.data.id
      // 更新配置实体信息
      currentConfigEntity.value = res.data.data
      // 更新URL，添加configId参数
      router.replace({ query: { configId: res.data.data.id } })
    } else {
      ElMessage.error(res.data.errorMessage || '保存失败')
    }
  } catch (e) {
    const errorMsg = e.response?.data?.errorMessage || e.message || '保存失败'
    ElMessage.error(errorMsg)
  } finally {
    saving.value = false
  }
}

// 从配置加载到画布
const loadConfigToCanvas = async (configId) => {
  try {
    const res = await configApi.getConfigById(configId)
    if (!res.data.success || !res.data.data) {
      ElMessage.error('加载配置失败')
      return
    }
    
    const entity = res.data.data
    // 保存配置实体，用于编辑时填充表单
    currentConfigEntity.value = entity
    
    const config = JSON.parse(entity.configContent)
    
    // 设置协议类型
    sourceProtocol.value = config.sourceProtocol || 'JSON'
    targetProtocol.value = config.targetProtocol || 'JSON'
    if (config.xmlRootElementName) {
      xmlRootElementName.value = config.xmlRootElementName
    }
    if (config.includeXmlDeclaration !== undefined) {
      includeXmlDeclaration.value = config.includeXmlDeclaration
    }
    
    // 清空画布
    if (graph) {
      graph.clearCells()
    }
    nodeCounter = 0
    autoLayoutCount = 0 // 重置布局计数器
    
    // 重建节点和边
    await nextTick()
    await loadRulesToCanvas(config.rules || [])
    
    ElMessage.success('配置加载成功')
  } catch (e) {
    console.error('加载配置失败:', e)
    ElMessage.error('加载配置失败: ' + (e.message || '未知错误'))
  }
}

// 将规则加载到画布
const loadRulesToCanvas = async (rules) => {
  if (!graph || !rules || rules.length === 0) return
  
  // 创建节点映射：sourcePath/targetPath -> nodeId
  const sourceNodeMap = new Map()
  const targetNodeMap = new Map()
  const COLUMN_WIDTH = 450
  const ROW_HEIGHT = 60
  let currentRow = 0
  
  // 第一步：创建所有节点
  rules.forEach((rule, index) => {
    // 处理源节点
    if (rule.sourcePath) {
      const sourcePath = rule.sourcePath.replace(/^\$\./, '')
      const sourcePathKey = sourcePath
      if (!sourceNodeMap.has(sourcePathKey)) {
        const nodeId = `s_${++nodeCounter}`
        const x = 50
        const y = 50 + (currentRow * ROW_HEIGHT)
        
        // 提取字段名（路径的最后一部分）
        const fieldName = sourcePath.split('.').pop() || sourcePath
        
        graph.addNode({
          id: nodeId,
          x: x,
          y: y,
          width: 140,
          height: 40,
          label: fieldName,
          data: { type: 'source', path: sourcePath },
          ports: {
            groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
            items: [{ id: 'p1', group: 'right' }]
          },
          attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
        })
        sourceNodeMap.set(sourcePathKey, nodeId)
      }
    }
    
    // 处理目标节点
    const targetPath = rule.targetPath
    const targetPathKey = targetPath
    if (!targetNodeMap.has(targetPathKey)) {
      const nodeId = `t_${++nodeCounter}`
      const x = 300
      const y = 50 + (currentRow * ROW_HEIGHT)
      
      // 提取字段名
      const fieldName = targetPath.split('.').pop() || targetPath
      
      graph.addNode({
        id: nodeId,
        x: x,
        y: y,
        width: 140,
        height: 40,
        label: fieldName,
        data: { type: 'target', path: targetPath },
        ports: {
          groups: { left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#9c27b0', fill: '#fff' } } } },
          items: [{ id: 'p1', group: 'left' }]
        },
        attrs: { body: { fill: '#f3e5f5', stroke: '#9c27b0', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
      })
      targetNodeMap.set(targetPathKey, nodeId)
      currentRow++
    }
    
    // 处理多对一映射的额外源
    if (rule.additionalSources && Array.isArray(rule.additionalSources)) {
      rule.additionalSources.forEach(additionalPath => {
        const sourcePathKey = additionalPath.replace(/^\$\./, '')
        if (!sourceNodeMap.has(sourcePathKey)) {
          const nodeId = `s_${++nodeCounter}`
          const x = 50
          const y = 50 + (currentRow * ROW_HEIGHT)
          const fieldName = sourcePathKey.split('.').pop() || sourcePathKey
          
          graph.addNode({
            id: nodeId,
            x: x,
            y: y,
            width: 140,
            height: 40,
            label: fieldName,
            data: { type: 'source', path: sourcePathKey },
            ports: {
              groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
              items: [{ id: 'p1', group: 'right' }]
            },
            attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
          })
          sourceNodeMap.set(sourcePathKey, nodeId)
        }
      })
    }
  })
  
  // 第二步：创建所有边
  rules.forEach((rule) => {
    if (rule.mappingType === 'MANY_TO_ONE' && rule.additionalSources) {
      // 多对一映射：主源路径 + 额外源路径 -> 目标
      const targetPathKey = rule.targetPath
      const targetNodeId = targetNodeMap.get(targetPathKey)
      
      if (targetNodeId && rule.sourcePath) {
        const sourcePathKey = rule.sourcePath.replace(/^\$\./, '')
        const sourceNodeId = sourceNodeMap.get(sourcePathKey)
        
        if (sourceNodeId) {
          graph.addEdge({
            source: { cell: sourceNodeId, port: 'p1' },
            target: { cell: targetNodeId, port: 'p1' },
            attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
            data: {
              mappingType: rule.mappingType,
              transformType: rule.transformType || 'GROOVY',
              transformConfig: rule.transformConfig || {}
            },
            labels: [{ attrs: { text: { text: rule.transformType || 'GROOVY', fontSize: 10 } } }]
          })
        }
        
        // 添加额外源的边
        rule.additionalSources.forEach(additionalPath => {
          const additionalPathKey = additionalPath.replace(/^\$\./, '')
          const additionalSourceNodeId = sourceNodeMap.get(additionalPathKey)
          if (additionalSourceNodeId) {
            graph.addEdge({
              source: { cell: additionalSourceNodeId, port: 'p1' },
              target: { cell: targetNodeId, port: 'p1' },
              attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
              data: {
                mappingType: 'MANY_TO_ONE',
                transformType: rule.transformType || 'GROOVY',
                transformConfig: rule.transformConfig || {}
              },
              labels: [{ attrs: { text: { text: rule.transformType || 'GROOVY', fontSize: 10 } } }]
            })
          }
        })
      }
    } else {
      // 一对一或一对多映射
      if (rule.sourcePath && rule.targetPath) {
        const sourcePathKey = rule.sourcePath.replace(/^\$\./, '')
        const targetPathKey = rule.targetPath
        const sourceNodeId = sourceNodeMap.get(sourcePathKey)
        const targetNodeId = targetNodeMap.get(targetPathKey)
        
        if (sourceNodeId && targetNodeId) {
          graph.addEdge({
            source: { cell: sourceNodeId, port: 'p1' },
            target: { cell: targetNodeId, port: 'p1' },
            attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
            data: {
              mappingType: rule.mappingType || 'ONE_TO_ONE',
              transformType: rule.transformType || 'DIRECT',
              transformConfig: rule.transformConfig || {}
            },
            labels: [{ attrs: { text: { text: rule.transformType || 'DIRECT', fontSize: 10 } } }]
          })
        }
      }
    }
  })
  
  // 更新布局计数器，确保后续手动添加节点时从正确位置继续
  autoLayoutCount = currentRow
}
</script>

<style scoped>
.canvas-editor { padding: 20px; }
.editor-card { height: calc(100vh - 100px); }
.editor-container { display: flex; gap: 20px; height: calc(100vh - 200px); }
.source-panel { width: 300px; display: flex; flex-direction: column; }
.canvas-panel { flex: 1; border: 1px solid #ddd; border-radius: 4px; overflow: hidden; position: relative; display: flex; flex-direction: column; }
.canvas-toolbar { padding: 10px; border-bottom: 1px solid #ddd; background-color: #fff; }
.graph-container { flex: 1; width: 100%; background-color: #fafafa; }
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