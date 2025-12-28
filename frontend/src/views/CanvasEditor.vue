<template>
  <div class="canvas-editor">
    <el-card class="editor-card">
<!--      <template #header>-->
<!--        <div class="card-header">-->
<!--          <span>报文映射画布编辑器</span>-->
<!--        </div>-->
<!--      </template>-->

      <div class="editor-container">
        <div class="source-panel" :class="{ 'collapsed': sourcePanelCollapsed }" :style="{ width: sourcePanelCollapsed ? '50px' : sourcePanelWidth + 'px' }">
          <div class="panel-header">
            <h3>源数据</h3>
            <el-button
              text
              :icon="sourcePanelCollapsed ? ArrowRight : ArrowLeft"
              @click="toggleSourcePanel"
              class="collapse-btn"
            />
          </div>
          <div v-show="!sourcePanelCollapsed" class="panel-content">
          <div style="margin-bottom: 10px; display: flex; gap: 10px; align-items: center;">
            <el-select v-model="sourceProtocol" style="flex: 1;" @change="onSourceProtocolChange">
              <el-option label="JSON" value="JSON" />
              <el-option label="XML" value="XML" />
            </el-select>
            <el-button 
              size="small" 
              @click="formatSourceData"
              :disabled="!sourceJson.trim()"
              title="格式化"
            >
              <el-icon><Setting /></el-icon>
              格式化
            </el-button>
          </div>
          
          <!-- 输入区域和树区域容器 -->
          <div class="source-content-wrapper">
            <!-- 输入区域 -->
            <div class="source-input-area" :style="{ height: sourceInputHeight + 'px' }">
              <!-- JSON 模式使用 Monaco Editor -->
              <div 
                v-if="sourceProtocol === 'JSON'"
                id="source-json-monaco-editor" 
                style="height: 100%; width: 100%; border: 1px solid #dcdfe6; border-radius: 4px;"
                :class="{ 'input-error': sourceParseError }"
              ></div>
              <!-- XML 模式使用普通 textarea -->
              <el-input
                  v-else
                  v-model="sourceJson"
                  type="textarea"
                  :rows="10"
                  placeholder="请输入源XML数据"
                  @input="parseSourceTree"
                  :class="{ 'input-error': sourceParseError }"
                  style="height: 100%;"
              />
              <div v-if="sourceParseError" style="color: #f56c6c; font-size: 12px; margin-top: 5px;">
                {{ sourceParseError }}
              </div>
            </div>
            
            <!-- 垂直分隔条 -->
            <div 
              class="resizer resizer-vertical"
              @mousedown="startVerticalResize($event)"
            ></div>
            
            <!-- 树区域 -->
            <div class="source-tree-area" style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
              <el-input
                  v-model="searchKeyword"
                  placeholder="搜索字段名（支持树和画布）..."
                  clearable
                  style="margin-bottom: 10px;"
                  @input="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-tree
                  ref="sourceTreeRef"
                  :data="filteredSourceTreeData"
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
                    <el-icon v-if="isFieldMapped(data.path)" class="mapped-icon" title="已映射">
                      <Check />
                    </el-icon>
                  </span>
                </template>
              </el-tree>
            </div>
          </div>
          </div>
        </div>
        
        <!-- 左侧分隔条 -->
        <div 
          v-if="!sourcePanelCollapsed"
          class="resizer resizer-left"
          @mousedown="startResize('left', $event)"
        ></div>

        <div
            ref="canvasPanel"
            class="canvas-panel"
            @dragover.prevent
            @drop="handleCanvasDrop"
        >
          <div class="canvas-toolbar">
            <el-button type="primary" size="small" @click="addNodePair">
              <el-icon><Plus /></el-icon>
              添加节点
            </el-button>
            <el-switch
              v-model="autoMappingMode"
              active-text="自动映射"
              inactive-text="手动映射"
              style="margin-left: 10px;"
              @change="onAutoMappingModeChange"
            />
            <el-button size="small" @click="zoomToFit">
              <el-icon><FullScreen /></el-icon>
              自适应视野
            </el-button>
            <el-button size="small" @click="zoomIn">
              <el-icon><ZoomIn /></el-icon>
              放大
            </el-button>
            <el-button size="small" @click="zoomOut">
              <el-icon><ZoomOut /></el-icon>
              缩小
            </el-button>
            <el-button type="primary" size="small"  @click="openSaveConfigDialog">{{ currentConfigId ? '修改配置' : '保存配置' }}</el-button>

          </div>
          <div ref="graphContainer" class="graph-container"></div>
          <div v-if="isCanvasEmpty" class="canvas-empty-tip">
            拖拽或双击字段到画布（也可在画布上直接点击"添加节点"按钮）
          </div>
        </div>
        
        <!-- 右侧分隔条 -->
        <div 
          v-if="!previewPanelCollapsed"
          class="resizer resizer-right"
          @mousedown="startResize('right', $event)"
        ></div>

        <div class="preview-panel" :class="{ 'collapsed': previewPanelCollapsed }" :style="{ width: previewPanelCollapsed ? '50px' : previewPanelWidth + 'px' }">
          <div class="panel-header">
            <h3>转换预览</h3>
            <el-button
              text
              :icon="previewPanelCollapsed ? ArrowLeft : ArrowRight"
              @click="togglePreviewPanel"
              class="collapse-btn"
            />
          </div>
          <div v-show="!previewPanelCollapsed" class="panel-content">
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
          <!-- 预览结果使用 Monaco Editor -->
          <div 
            id="preview-result-monaco-editor" 
            style="height: 400px; width: 100%; border: 1px solid #dcdfe6; border-radius: 4px;"
          ></div>
          <div v-if="previewError" style="margin-top: 10px; color: #f56c6c; font-size: 12px;">
            {{ previewError }}
          </div>
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

    <!-- 添加节点对话框 -->
    <el-dialog v-model="addNodeDialogVisible" title="添加节点" width="600px">
      <el-form :model="addNodeForm" label-width="120px">
        <el-divider content-position="left">输入字段</el-divider>
        <el-form-item label="输入字段名" required>
          <el-input v-model="addNodeForm.fieldName" placeholder="请输入输入字段名称" />
        </el-form-item>
        <el-form-item label="输入路径（可选）">
          <el-input v-model="addNodeForm.sourcePath" placeholder="留空则使用输入字段名作为路径" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            例如：$.user.name 或 user.name
          </div>
        </el-form-item>
        <el-divider content-position="left">输出字段</el-divider>
        <el-form-item label="输出字段名" required>
          <el-input v-model="addNodeForm.targetFieldName" placeholder="请输入输出字段名称" />
        </el-form-item>
        <el-form-item label="输出路径（可选）">
          <el-input v-model="addNodeForm.targetPath" placeholder="留空则使用输出字段名作为路径" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            例如：user.name 或 address.city
          </div>
        </el-form-item>
        <el-divider content-position="left">映射规则</el-divider>
        <el-form-item label="映射规则">
          <el-select v-model="addNodeForm.mappingType" style="width: 100%;">
            <el-option label="一对一 (ONE_TO_ONE)" value="ONE_TO_ONE" />
            <el-option label="一对多 (ONE_TO_MANY)" value="ONE_TO_MANY" />
            <el-option label="多对一 (MANY_TO_ONE)" value="MANY_TO_ONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="转换类型">
          <el-select v-model="addNodeForm.transformType" style="width: 100%;">
            <el-option label="直接映射 (DIRECT)" value="DIRECT" />
            <el-option label="Groovy脚本 (GROOVY)" value="GROOVY" />
            <el-option label="字典转换 (DICTIONARY)" value="DICTIONARY" />
            <el-option label="函数转换 (FUNCTION)" value="FUNCTION" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addNodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddNode">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="edgeConfigVisible"
      title="配置转换规则"
      size="50%"
      :append-to-body="true"
      :trap-focus="false"
      :destroy-on-close="true"
      @opened="handleDrawerOpened"
    >
      <el-form :model="currentEdgeConfig" label-width="120px" style="padding: 20px;">
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
          <el-select 
            v-model="currentEdgeConfig.transformConfig.function" 
            placeholder="请选择函数"
            style="width: 100%"
          >
            <el-option
              v-for="(desc, code) in availableFunctions"
              :key="code"
              :label="desc"
              :value="code"
            />
          </el-select>
        </el-form-item>
        
        <!-- Groovy脚本配置 -->
        <el-form-item v-show="currentEdgeConfig.transformType === 'GROOVY'" label="Groovy脚本">
          <div 
            id="groovy-monaco-editor" 
            style="height: 400px; width: 100%; border: 1px solid #dcdfe6; border-radius: 4px; pointer-events: auto !important;"
          ></div>
          <div style="font-size: 12px; color: #999; margin-top: 5px;">
            <div><strong>变量说明：</strong></div>
            <div>• input: 输入的字段值（单个值或List）</div>
            <div>• inputs: 当输入是List时的别名</div>
            <div style="margin-top: 5px;"><strong>示例：</strong></div>
            <div>• 去掉邮箱@后面的部分: <code>input?.toString()?.split('@')?[0] ?: ''</code></div>
            <div>• 字符串拼接(多对1): <code>def parts = input as List; return (parts[0] ?: '') + ' ' + (parts[1] ?: '')</code></div>
            <div>• 取前三位转为数字: <code>String str = input?.toString() ?: ''; return Integer.parseInt(str.length() >= 3 ? str.substring(0, 3) : str)</code></div>
          </div>
        </el-form-item>
        
        <!-- 字典映射配置 -->
        <el-form-item v-if="currentEdgeConfig.transformType === 'DICTIONARY'" label="选择字典">
          <el-select 
            v-model="currentEdgeConfig.dictionaryId" 
            placeholder="请选择字典"
            style="width: 100%"
            @change="onDictionaryChange"
          >
            <el-option
              v-for="dict in dictionaryList"
              :key="dict.id"
              :label="`${dict.name} (${dict.code})`"
              :value="Number(dict.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="currentEdgeConfig.transformType === 'DICTIONARY' && currentEdgeConfig.dictionaryId" label="转换方向">
          <el-radio-group v-model="currentEdgeConfig.dictionaryDirection">
            <el-radio :label="false">键转值 (K->V)</el-radio>
            <el-radio :label="true">值转键 (V->K)</el-radio>
          </el-radio-group>
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
        <div style="flex: auto; padding: 20px;">
          <el-button @click="edgeConfigVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdgeConfig">确定保存配置</el-button>
        </div>
      </template>
    </el-drawer>

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

    <!-- 右键菜单 -->
    <div
      v-if="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div
        v-if="contextMenuType === 'node'"
        class="context-menu-item"
        @click="handleContextMenuEdit"
      >
        <el-icon><Edit /></el-icon>
        <span>编辑</span>
      </div>
      <div
        v-if="contextMenuType === 'edge'"
        class="context-menu-item"
        @click="handleContextMenuEdit"
      >
        <el-icon><Setting /></el-icon>
        <span>编辑规则</span>
      </div>
      <div class="context-menu-divider" v-if="contextMenuType === 'node' || contextMenuType === 'edge'"></div>
      <div
        class="context-menu-item context-menu-item-danger"
        @click="handleContextMenuDelete"
      >
        <el-icon><Delete /></el-icon>
        <span>删除</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, watch, computed } from 'vue'
import { Graph } from '@antv/x6'
import { Selection } from '@antv/x6-plugin-selection'
import { MiniMap } from '@antv/x6-plugin-minimap'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Delete, Plus, ArrowLeft, ArrowRight, FullScreen, ZoomIn, ZoomOut, Search, Check, Edit, Setting } from '@element-plus/icons-vue'
import { transformV2Api, configApi, dictionaryApi, functionApi } from '../api'
import { useRoute, useRouter } from 'vue-router'
import loader from '@monaco-editor/loader'

// --- 状态变量 ---
const sourceJson = ref('')
const sourceTreeData = ref([])
const sourceParseError = ref('') // 源数据解析错误信息
const searchKeyword = ref('') // 搜索关键词
const sourceProtocol = ref('JSON') // 源协议类型：JSON 或 XML
const targetProtocol = ref('JSON') // 目标协议类型：JSON 或 XML
const sourcePanelCollapsed = ref(false) // 左侧面板折叠状态，默认展开
const previewPanelCollapsed = ref(false) // 右侧面板折叠状态，默认展开
const sourcePanelWidth = ref(300) // 源数据面板宽度
const previewPanelWidth = ref(350) // 预览面板宽度
const sourceInputHeight = ref(200) // 源数据输入框高度
const autoMappingMode = ref(false) // 自动映射模式
const nodeCount = ref(0) // 节点数量，用于判断画布是否为空
const isCanvasEmpty = computed(() => nodeCount.value === 0)

// 拖动调整面板宽度
let isResizingPanels = false
let resizeDirection = null // 'left' 或 'right'
let startX = 0
let startWidth = 0

// 拖动调整输入框高度
let isResizingVertical = false
let startY = 0
let startHeight = 0

const startResize = (direction, e) => {
  isResizingPanels = true
  resizeDirection = direction
  startX = e.clientX
  if (direction === 'left') {
    startWidth = sourcePanelWidth.value
  } else {
    startWidth = previewPanelWidth.value
  }
  
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

const handleResize = (e) => {
  if (!isResizingPanels) return
  
  const deltaX = e.clientX - startX
  const minWidth = 200 // 最小宽度
  const maxWidth = 800 // 最大宽度
  
  if (resizeDirection === 'left') {
    const newWidth = startWidth + deltaX
    sourcePanelWidth.value = Math.max(minWidth, Math.min(maxWidth, newWidth))
  } else {
    const newWidth = startWidth - deltaX
    previewPanelWidth.value = Math.max(minWidth, Math.min(maxWidth, newWidth))
  }
}

const stopResize = () => {
  isResizingPanels = false
  resizeDirection = null
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

// 开始垂直调整（输入框高度）
const startVerticalResize = (e) => {
  isResizingVertical = true
  startY = e.clientY
  startHeight = sourceInputHeight.value
  
  document.addEventListener('mousemove', handleVerticalResize)
  document.addEventListener('mouseup', stopVerticalResize)
  e.preventDefault()
}

const handleVerticalResize = (e) => {
  if (!isResizingVertical) return
  
  const deltaY = e.clientY - startY
  const minHeight = 100 // 最小高度
  const maxHeight = 500 // 最大高度
  
  const newHeight = startHeight + deltaY
  sourceInputHeight.value = Math.max(minHeight, Math.min(maxHeight, newHeight))
  
  // 更新 Monaco Editor 布局
  if (sourceJsonEditor) {
    sourceJsonEditor.layout()
  }
}

const stopVerticalResize = () => {
  isResizingVertical = false
  document.removeEventListener('mousemove', handleVerticalResize)
  document.removeEventListener('mouseup', stopVerticalResize)
}
const xmlRootElementName = ref('') // XML根元素名称
const includeXmlDeclaration = ref(false) // 是否包含XML声明
const graphContainer = ref(null)
const canvasPanel = ref(null)
let graph = null
let minimap = null // 小地图实例
let groovyEditor = null // Monaco Editor 实例（Groovy脚本）
let sourceJsonEditor = null // Monaco Editor 实例（JSON输入）
let previewResultEditor = null // Monaco Editor 实例（预览结果）
let isMonacoProviderRegistered = false // Monaco Provider 是否已注册

// 注册 Monaco 代码提示提供器（全局只注册一次）
const registerMonacoProviders = async (monaco) => {
  if (isMonacoProviderRegistered) return
  
  // 1. 注册基础类型提示
  monaco.languages.typescript.javascriptDefaults.addExtraLib(`
    declare var input: any;
    declare var inputs: any[];
    interface String { 
      split(sep: string): string[]; 
      substring(s: number, e?: number): string;
      toString(): string;
      length: number;
      toLowerCase(): string;
      toUpperCase(): string;
      trim(): string;
      replace(searchValue: string, replaceValue: string): string;
    }
    interface Array<T> {
      length: number;
      map<U>(callback: (value: T) => U): U[];
      filter(callback: (value: T) => boolean): T[];
      join(separator?: string): string;
      forEach(callback: (value: T) => void): void;
    }
  `, 'groovy-lib.d.ts')

  // 2. 设置编译选项
  monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
    target: monaco.languages.typescript.ScriptTarget.ES2020,
    allowNonTsExtensions: true,
    checkJs: false
  })

  // 3. 注册自定义联想提示
  monaco.languages.registerCompletionItemProvider('javascript', {
    triggerCharacters: ['.'], // 关键：按下点号时触发
    provideCompletionItems: (model, position) => {
      const lineContent = model.getLineContent(position.lineNumber)
      const textBeforeCursor = lineContent.substring(0, position.column - 1)
      
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn
      }

      // 如果当前行以 input. 结尾
      if (textBeforeCursor.endsWith('input.')) {
        return {
          suggestions: [
            { 
              label: 'toString()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '将值转换为字符串',
              insertText: 'toString()', 
              range 
            },
            { 
              label: 'split()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '字符串分割：input.split("-")',
              insertText: 'split(\'${1:separator}\')', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            },
            { 
              label: 'substring()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '截取子字符串：input.substring(0, 3)',
              insertText: 'substring(${1:start}, ${2:end})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            },
            { 
              label: 'trim()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '去除首尾空格',
              insertText: 'trim()', 
              range 
            },
            { 
              label: 'toLowerCase()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '转换为小写',
              insertText: 'toLowerCase()', 
              range 
            },
            { 
              label: 'toUpperCase()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '转换为大写',
              insertText: 'toUpperCase()', 
              range 
            },
            { 
              label: 'length', 
              kind: monaco.languages.CompletionItemKind.Property, 
              documentation: '获取字符串长度',
              insertText: 'length', 
              range 
            },
            { 
              label: 'replace()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '字符串替换：input.replace("old", "new")',
              insertText: 'replace(${1:searchValue}, ${2:replaceValue})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            }
          ]
        }
      }
      
      // 如果当前行以 inputs. 结尾
      if (textBeforeCursor.endsWith('inputs.')) {
        return {
          suggestions: [
            { 
              label: 'length', 
              kind: monaco.languages.CompletionItemKind.Property, 
              documentation: '数组长度',
              insertText: 'length', 
              range 
            },
            { 
              label: 'map()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '数组映射：inputs.map { it.toString() }',
              insertText: 'map(${1:callback})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            },
            { 
              label: 'filter()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '数组过滤：inputs.filter { it != null }',
              insertText: 'filter(${1:callback})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            },
            { 
              label: 'join()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '数组连接：inputs.join(",")',
              insertText: 'join(${1:separator})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            },
            { 
              label: 'forEach()', 
              kind: monaco.languages.CompletionItemKind.Method, 
              documentation: '遍历数组：inputs.forEach { ... }',
              insertText: 'forEach(${1:callback})', 
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, 
              range 
            }
          ]
        }
      }
      
      return { suggestions: [] }
    }
  })
  
  isMonacoProviderRegistered = true
}

// 右键菜单相关
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuType = ref('') // 'node' 或 'edge'
let contextMenuTarget = null // 当前右键点击的目标（节点或边）
let nodeCounter = 0
let autoLayoutCount = 0 // 用于双击排版的计数器

const previewResult = ref('')
const previewError = ref('')
const previewing = ref(false)
let keyDownHandler = null

const treeProps = { children: 'children', label: 'label' }
const edgeConfigVisible = ref(false)
const nodeEditVisible = ref(false)
const addNodeDialogVisible = ref(false)
let currentNodeToEdit = null
const currentNodeEdit = ref({ fieldName: '', path: '' })
const currentNodeEditType = ref('') // 'source' 或 'target'
const addNodeForm = ref({
  fieldName: '',
  sourcePath: '',
  targetFieldName: '',
  targetPath: '',
  mappingType: 'ONE_TO_ONE',
  transformType: 'DIRECT'
})
const currentEdgeConfig = ref({ 
  sourcePath: '', 
  targetPath: '', 
  mappingType: 'ONE_TO_ONE',
  transformType: 'DIRECT', 
  transformConfig: {},
  dictionaryId: null,
  dictionaryDirection: false // false: k->v, true: v->k
})
const dictionaryList = ref([])
const availableFunctions = ref({}) // 可用函数列表（包括系统函数和自定义函数）
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

// 使用 ResizeObserver 监听画布容器大小变化
let resizeObserver = null

// 定义一个用于平滑缩放的工具函数
// 添加标志防止无限循环
let isResizing = false

const syncGraphResize = () => {
  if (!graph || !graphContainer.value || isResizing) return
  
  isResizing = true
  // 使用 nextTick 确保 DOM 状态已同步
  nextTick(() => {
    try {
      const rect = graphContainer.value.getBoundingClientRect()
      if (rect.width > 0 && rect.height > 0) {
        // 核心：先 resize，再强制画布刷新
        graph.resize(rect.width, rect.height)
      }
    } finally {
      // 使用 setTimeout 确保在下一个事件循环中重置标志
      setTimeout(() => {
        isResizing = false
      }, 0)
    }
  })
}

// 创建一个通用的强制重绘函数（保留作为备用）
const forceUpdateCanvas = () => {
  syncGraphResize()
  if (graph) {
    graph.centerContent()
  }
}

// 修改折叠/展开的点击逻辑
// 手动在更新状态后，利用一个微小的延迟（setTimeout 0）来跨过浏览器的重绘周期
const toggleSourcePanel = () => {
  sourcePanelCollapsed.value = !sourcePanelCollapsed.value
  // 使用 setTimeout 确保在 DOM 彻底渲染后执行
  setTimeout(() => {
    syncGraphResize()
    // 侧边栏切换后，不仅居中，还自动适应一下视野
    if (graph) {
      zoomToFit()
    }
  }, 0)
}

const togglePreviewPanel = () => {
  previewPanelCollapsed.value = !previewPanelCollapsed.value
  setTimeout(() => {
    syncGraphResize()
    // 侧边栏切换后，不仅居中，还自动适应一下视野
    if (graph) {
      zoomToFit()
    }
  }, 0)
}

// 窗口大小变化时也调整画布大小
const handleWindowResize = () => {
  syncGraphResize()
}

onMounted(() => {
  // 监听文档点击事件，关闭右键菜单
  document.addEventListener('click', handleDocumentClick)
  
  nextTick(() => {
    initGraph()
    // 加载字典列表和函数列表
    loadDictionaryList()
    loadAvailableFunctions()
    // 注意：Monaco Editor 将在对话框打开且类型为 GROOVY 时初始化
    // 初始化 JSON 编辑器（如果默认是 JSON）
    if (sourceProtocol.value === 'JSON') {
      initSourceJsonEditor()
    }
    // 初始化预览结果编辑器
    initPreviewResultEditor()
    // 检查URL参数，如果是编辑模式则加载配置
    const configId = route.query.configId
    if (configId) {
      currentConfigId.value = configId
      loadConfigToCanvas(Number(configId))
    }
    
    // 设置 ResizeObserver 监听画布容器大小变化
    // 这里的回调主要处理非折叠引起的尺寸变化（如窗口缩放）
    if (canvasPanel.value && window.ResizeObserver) {
      resizeObserver = new ResizeObserver(() => {
        syncGraphResize()
      })
      resizeObserver.observe(canvasPanel.value)
    }
    
    // 监听窗口大小变化
    window.addEventListener('resize', handleWindowResize)
  })
})

// 注意：现在不再需要 watch，因为我们在点击按钮时直接调用 forceUpdateCanvas
// 但保留 watch 作为备用，以防其他地方直接修改了 collapsed 状态
watch([sourcePanelCollapsed, previewPanelCollapsed], () => {
  // 延迟一点确保 DOM 更新完成
  nextTick(() => {
    forceUpdateCanvas()
  })
})

// 监听类型切换，如果是手动切换到 GROOVY，也调用初始化
watch(() => currentEdgeConfig.value.transformType, (newType) => {
  if (newType === 'GROOVY' && edgeConfigVisible.value) {
    nextTick(() => initGroovyEditor())
  }
})

// 监听协议类型切换
watch(() => sourceProtocol.value, (newProtocol) => {
  if (newProtocol === 'JSON') {
    nextTick(() => {
      initSourceJsonEditor()
    })
  } else {
    // 切换到 XML，销毁 JSON 编辑器
    if (sourceJsonEditor) {
      sourceJsonEditor.dispose()
      sourceJsonEditor = null
    }
  }
})

// 监听目标协议类型切换，更新预览编辑器语言
watch(() => targetProtocol.value, () => {
  updatePreviewEditor()
})

onUnmounted(() => {
  if (keyDownHandler) window.removeEventListener('keydown', keyDownHandler)
  window.removeEventListener('resize', handleWindowResize)
  document.removeEventListener('click', handleDocumentClick)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (groovyEditor) {
    groovyEditor.dispose()
    groovyEditor = null
  }
  if (sourceJsonEditor) {
    sourceJsonEditor.dispose()
    sourceJsonEditor = null
  }
  if (previewResultEditor) {
    previewResultEditor.dispose()
    previewResultEditor = null
  }
  if (minimap) {
    if (minimap.container && minimap.container.parentNode) {
      minimap.container.parentNode.removeChild(minimap.container)
    }
    minimap = null
  }
  if (graph) {
    graph.dispose()
    graph = null
  }
})

// 初始化 Groovy 编辑器（Monaco Editor）
const initGroovyEditor = async () => {
  if (groovyEditor) {
    groovyEditor.dispose()
    groovyEditor = null
  }

  setTimeout(async () => {
    const domElement = document.getElementById('groovy-monaco-editor')
    if (!domElement) {
      console.warn('Groovy 编辑器容器未找到')
      return
    }

    try {
      const monaco = await loader.init()
      
      // 执行注册逻辑（全局只注册一次）
      await registerMonacoProviders(monaco)
      
      groovyEditor = monaco.editor.create(domElement, {
        value: currentEdgeConfig.value.transformConfig?.groovyScript || 'return input',
        language: 'javascript',
        theme: 'vs',
        automaticLayout: true,
        fixedOverflowWidgets: true, // 必须：防止提示框被截断
        suggestOnTriggerCharacters: true, // 必须：输入 . 时触发提示
        quickSuggestions: { other: true, comments: false, strings: true }, // 必须：开启快速提示
        acceptSuggestionOnEnter: 'on',
        parameterHints: { enabled: true },
        wordBasedSuggestions: true,
        lineNumbers: 'on',
        renderLineHighlight: 'all',
        selectOnLineNumbers: true,
        readOnly: false,
        accessibilitySupport: 'on'
      })

      groovyEditor.onDidChangeModelContent(() => {
        if (groovyEditor) {
          const val = groovyEditor.getValue()
          if (!currentEdgeConfig.value.transformConfig) {
            currentEdgeConfig.value.transformConfig = {}
          }
          currentEdgeConfig.value.transformConfig.groovyScript = val
        }
      })

      domElement.addEventListener('mousedown', () => {
        if (groovyEditor) {
          groovyEditor.focus()
        }
      }, true)

      groovyEditor.layout()
      groovyEditor.focus()
    } catch (e) {
      console.error("Monaco load error:", e)
    }
  }, 400)
}

// 抽屉打开后的回调
const handleDrawerOpened = () => {
  if (currentEdgeConfig.value.transformType === 'GROOVY') {
    initGroovyEditor()
  }
}

// 初始化 JSON 编辑器（Monaco Editor）
const initSourceJsonEditor = async () => {
  if (sourceJsonEditor) {
    sourceJsonEditor.dispose()
    sourceJsonEditor = null
  }

  await nextTick()
  
  setTimeout(async () => {
    const domElement = document.getElementById('source-json-monaco-editor')
    if (!domElement) {
      return
    }

    try {
      const monaco = await loader.init()
      
      sourceJsonEditor = monaco.editor.create(domElement, {
        value: sourceJson.value || '',
        language: 'json',
        theme: 'vs',
        automaticLayout: true,
        fixedOverflowWidgets: true,
        lineNumbers: 'on',
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        readOnly: false,
        formatOnPaste: true,
        formatOnType: false,
        wordWrap: 'on',
        fontSize: 14
      })

      // 监听内容变化，同步到 sourceJson
      sourceJsonEditor.onDidChangeModelContent(() => {
        if (sourceJsonEditor) {
          const val = sourceJsonEditor.getValue()
          sourceJson.value = val
          parseSourceTree()
        }
      })

      // 监听粘贴事件，自动格式化
      sourceJsonEditor.onDidPaste(() => {
        setTimeout(() => {
          if (sourceJsonEditor && sourceProtocol.value === 'JSON') {
            try {
              const content = sourceJsonEditor.getValue()
              const parsed = JSON.parse(content)
              const formatted = JSON.stringify(parsed, null, 2)
              sourceJsonEditor.setValue(formatted)
              ElMessage.success('已自动格式化 JSON')
            } catch (e) {
              // 格式化失败时不提示
            }
          }
        }, 10)
      })

      // 点击容器时聚焦
      domElement.addEventListener('mousedown', () => {
        if (sourceJsonEditor) {
          sourceJsonEditor.focus()
        }
      }, true)

      sourceJsonEditor.layout()
    } catch (e) {
      console.error("Monaco JSON Editor load error:", e)
    }
  }, 100)
}

// 初始化预览结果编辑器（Monaco Editor）
const initPreviewResultEditor = async () => {
  if (previewResultEditor) {
    previewResultEditor.dispose()
    previewResultEditor = null
  }

  await nextTick()
  
  setTimeout(async () => {
    const domElement = document.getElementById('preview-result-monaco-editor')
    if (!domElement) {
      return
    }

    try {
      const monaco = await loader.init()
      
      // 根据目标协议确定语言
      const language = targetProtocol.value === 'JSON' ? 'json' : 'xml'
      
      previewResultEditor = monaco.editor.create(domElement, {
        value: previewResult.value || '',
        language: language,
        theme: 'vs',
        automaticLayout: true,
        fixedOverflowWidgets: true,
        lineNumbers: 'on',
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        readOnly: true, // 预览框只读
        wordWrap: 'on',
        fontSize: 14
      })

      previewResultEditor.layout()
    } catch (e) {
      console.error("Monaco Preview Editor load error:", e)
    }
  }, 100)
}

// 更新预览编辑器内容
const updatePreviewEditor = () => {
  if (!previewResultEditor) return
  
  const language = targetProtocol.value === 'JSON' ? 'json' : 'xml'
  const model = previewResultEditor.getModel()
  
  // 更新语言模式（使用全局 monaco API）
  if (model && window.monaco) {
    window.monaco.editor.setModelLanguage(model, language)
  }
  
  // 更新内容
  const currentValue = previewResultEditor.getValue()
  if (currentValue !== previewResult.value) {
    previewResultEditor.setValue(previewResult.value || '')
    // 格式化代码
    setTimeout(() => {
      if (previewResultEditor) {
        previewResultEditor.getAction('editor.action.formatDocument').run().catch(() => {
          // 格式化失败时忽略错误
        })
      }
    }, 100)
  }
}

const initGraph = () => {
  if (!graphContainer.value) return

  // 获取容器尺寸
  const rect = graphContainer.value.getBoundingClientRect()
  
  graph = new Graph({
    container: graphContainer.value,
    width: rect.width || 800,
    height: rect.height || 600,
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

  // 初始化小地图（默认隐藏，当边数超过20时显示）
  // 创建小地图容器（初始隐藏）
  const minimapContainer = document.createElement('div')
  minimapContainer.className = 'x6-minimap-container'
  minimapContainer.style.cssText = `
    position: absolute;
    bottom: 10px;
    right: 10px;
    width: 200px;
    height: 160px;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    background-color: #fff;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    z-index: 100;
    display: none;
  `
  graphContainer.value.appendChild(minimapContainer)
  
  minimap = new MiniMap({
    container: minimapContainer,
    width: 200,
    height: 160,
    padding: 10,
  })
  
  graph.use(minimap)
  
  // 设置小地图位置（右下角）
  const updateMinimapVisibility = () => {
    if (!graph || !minimap || !minimapContainer) return
    
    const edges = graph.getEdges()
    const shouldShow = edges.length > 20
    
    // 显示或隐藏小地图
    minimapContainer.style.display = shouldShow ? 'block' : 'none'
  }
  
  // 初始检查
  updateMinimapVisibility()

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
  graph.on('edge:connected', () => {
    updateMinimapVisibility() // 更新小地图显示状态
    nextTick(() => updatePreview())
  })
  graph.on('node:removed', () => {
    nodeCount.value = graph.getNodes().length
    updateMappedPaths()
    updateMinimapVisibility() // 更新小地图显示状态
    nextTick(() => updatePreview())
  })
  graph.on('edge:removed', ({ edge }) => {
    // 清理保存的样式
    if (edge && edge.id) {
      edgeOriginalStyles.delete(edge.id)
    }
    updateMinimapVisibility() // 更新小地图显示状态
    nextTick(() => updatePreview())
  })
  
  // 监听节点添加事件，更新映射状态
  graph.on('node:added', () => {
    nodeCount.value = graph.getNodes().length
    updateMappedPaths()
    updateMinimapVisibility() // 更新小地图显示状态
  })
  
  // --- 线条高亮功能 ---
  // 保存边的原始样式
  const edgeOriginalStyles = new Map()
  
  // 高亮边
  const highlightEdge = (edge) => {
    if (!edge) return
    
    // 保存原始样式（如果还没有保存）
    if (!edgeOriginalStyles.has(edge.id)) {
      const attrs = edge.getAttrs()
      edgeOriginalStyles.set(edge.id, {
        stroke: attrs.line?.stroke || '#8f8f8f',
        strokeWidth: attrs.line?.strokeWidth || 2
      })
    }
    
    // 高亮显示（加粗并改变颜色）
    edge.setAttrs({
      line: {
        stroke: '#409eff', // 蓝色高亮
        strokeWidth: 4 // 加粗
      }
    })
  }
  
  // 恢复边的原始样式
  const restoreEdge = (edge) => {
    if (!edge) return
    
    const originalStyle = edgeOriginalStyles.get(edge.id)
    if (originalStyle) {
      edge.setAttrs({
        line: {
          stroke: originalStyle.stroke,
          strokeWidth: originalStyle.strokeWidth
        }
      })
    } else {
      // 如果没有保存的样式，使用默认样式
      edge.setAttrs({
        line: {
          stroke: '#8f8f8f',
          strokeWidth: 2
        }
      })
    }
  }
  
  // 高亮与节点相关的所有边
  const highlightNodeEdges = (node) => {
    if (!node) return
    
    const nodeId = node.id
    const allEdges = graph.getEdges()
    
    allEdges.forEach(edge => {
      const sourceCellId = edge.getSourceCellId()
      const targetCellId = edge.getTargetCellId()
      
      // 如果边连接到该节点，则高亮
      if (sourceCellId === nodeId || targetCellId === nodeId) {
        highlightEdge(edge)
      }
    })
  }
  
  // 恢复所有边的原始样式
  const restoreAllEdges = () => {
    const allEdges = graph.getEdges()
    allEdges.forEach(edge => {
      restoreEdge(edge)
    })
  }
  
  // 监听边的鼠标悬停事件
  graph.on('edge:mouseenter', ({ edge }) => {
    highlightEdge(edge)
  })
  
  graph.on('edge:mouseleave', ({ edge }) => {
    // 只有在没有选中节点时才恢复（避免与选中高亮冲突）
    const selectedCells = graph.getSelectedCells()
    const hasSelectedNode = selectedCells.some(cell => cell.isNode())
    
    if (!hasSelectedNode) {
      restoreEdge(edge)
    }
  })
  
  // 监听节点选中事件
  graph.on('cell:selected', ({ cell }) => {
    if (cell.isNode()) {
      // 高亮与选中节点相关的所有边
      highlightNodeEdges(cell)
    } else if (cell.isEdge()) {
      // 如果选中的是边，也高亮该边
      highlightEdge(cell)
    }
  })
  
  // 监听节点取消选中事件
  graph.on('cell:unselected', ({ cell }) => {
    // 恢复所有边的样式
    restoreAllEdges()
  })
  
  // --- 右键菜单功能 ---
  // 监听节点的右键点击事件
  graph.on('node:contextmenu', ({ node, e }) => {
    e.preventDefault()
    e.stopPropagation()
    
    contextMenuTarget = node
    contextMenuType.value = 'node'
    contextMenuX.value = e.clientX
    contextMenuY.value = e.clientY
    contextMenuVisible.value = true
  })
  
  // 监听边的右键点击事件
  graph.on('edge:contextmenu', ({ edge, e }) => {
    e.preventDefault()
    e.stopPropagation()
    
    contextMenuTarget = edge
    contextMenuType.value = 'edge'
    contextMenuX.value = e.clientX
    contextMenuY.value = e.clientY
    contextMenuVisible.value = true
  })
  
  // 点击其他地方时关闭菜单
  graph.on('blank:click', () => {
    contextMenuVisible.value = false
  })

  keyDownHandler = (e) => {
    const active = document.activeElement
    
    // 严谨判定：是否在 Monaco 的文本输入区
    const isMonacoActive = 
      active.closest('.monaco-editor') || 
      active.classList.contains('monaco-mouse-cursor-text') ||
      active.closest('.monaco-aria-container')

    // 发现输入焦点，立即交还控制权给浏览器
    if (isMonacoActive || active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable) {
      return
    }

    // --- 以下才是画布操作逻辑 ---
    // Ctrl + Z (撤销)
    if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
      e.preventDefault()
      undo()
      return
    }
    // Ctrl + Y 或 Ctrl + Shift + Z (重做)
    if ((e.ctrlKey || e.metaKey) && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) {
      e.preventDefault()
      redo()
      return
    }

    // 原有的 Delete 逻辑（只在画布上生效）
    if (e.key === 'Delete' || e.key === 'Backspace') {
      const selected = graph.getSelectedCells()
      if (selected.length > 0) {
        e.preventDefault()
        graph.removeCells(selected)
      }
    }
  }
  window.addEventListener('keydown', keyDownHandler)
}

// --- 树逻辑 ---
const parseSourceTree = () => {
  // 清空之前的错误信息
  sourceParseError.value = ''
  
  try {
    if (!sourceJson.value.trim()) { 
      sourceTreeData.value = []
      return 
    }
    
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
    
    // 如果启用了自动映射模式，自动生成映射规则
    if (autoMappingMode.value && sourceProtocol.value === 'JSON') {
      nextTick(() => {
        generateAutoMapping()
      })
    }
  } catch (e) { 
    console.error('解析失败:', e)
    sourceTreeData.value = []
    
    // 设置错误信息
    let errorMessage = '解析失败: '
    if (sourceProtocol.value === 'XML') {
      errorMessage += e.message || 'XML格式错误，请检查XML语法'
    } else {
      if (e instanceof SyntaxError) {
        errorMessage += 'JSON格式错误，请检查JSON语法'
      } else {
        errorMessage += e.message || '未知错误'
      }
    }
    sourceParseError.value = errorMessage
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
  // 切换协议类型时，清空错误信息并重新解析
  sourceParseError.value = ''
  parseSourceTree()
}

// 格式化源数据
const formatSourceData = () => {
  if (!sourceJson.value.trim()) {
    ElMessage.warning('请输入要格式化的数据')
    return
  }
  
  try {
    if (sourceProtocol.value === 'JSON') {
      // JSON 格式化
      const parsed = JSON.parse(sourceJson.value)
      const formatted = JSON.stringify(parsed, null, 2)
      sourceJson.value = formatted
      
      // 如果使用 Monaco Editor，更新编辑器内容
      if (sourceJsonEditor) {
        sourceJsonEditor.setValue(formatted)
        // 格式化代码
        sourceJsonEditor.getAction('editor.action.formatDocument').run()
      }
      
      ElMessage.success('JSON 格式化成功')
    } else {
      // XML 格式化
      sourceJson.value = formatXml(sourceJson.value)
      ElMessage.success('XML 格式化成功')
    }
    // 格式化后重新解析
    parseSourceTree()
  } catch (e) {
    ElMessage.error('格式化失败: ' + (e.message || '格式错误'))
  }
}

// XML 格式化函数
const formatXml = (xmlString) => {
  try {
    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml')
    
    // 检查是否有解析错误
    const parseError = xmlDoc.querySelector('parsererror')
    if (parseError) {
      throw new Error('XML格式错误')
    }
    
    // 格式化 XML
    const formatNode = (node, indent = '') => {
      let result = ''
      
      if (node.nodeType === 1) { // 元素节点
        const nodeName = node.nodeName
        const attributes = Array.from(node.attributes)
          .map(attr => ` ${attr.name}="${attr.value}"`)
          .join('')
        
        const children = Array.from(node.childNodes).filter(
          child => child.nodeType === 1 || (child.nodeType === 3 && child.textContent.trim())
        )
        
        if (children.length === 0) {
          // 自闭合标签或空标签
          const textContent = node.textContent?.trim()
          if (textContent) {
            result = `${indent}<${nodeName}${attributes}>${textContent}</${nodeName}>`
          } else {
            result = `${indent}<${nodeName}${attributes} />`
          }
        } else {
          // 有子节点
          result = `${indent}<${nodeName}${attributes}>\n`
          children.forEach(child => {
            if (child.nodeType === 1) {
              result += formatNode(child, indent + '  ') + '\n'
            } else if (child.nodeType === 3 && child.textContent.trim()) {
              result += `${indent}  ${child.textContent.trim()}\n`
            }
          })
          result += `${indent}</${nodeName}>`
        }
      }
      
      return result
    }
    
    // 获取根元素
    const rootElement = xmlDoc.documentElement
    if (!rootElement) {
      throw new Error('找不到根元素')
    }
    
    // 检查是否有 XML 声明
    const hasDeclaration = xmlString.trim().startsWith('<?xml')
    let declaration = ''
    if (hasDeclaration) {
      const declarationMatch = xmlString.match(/^<\?xml[^>]*\?>/)
      if (declarationMatch) {
        declaration = declarationMatch[0] + '\n'
      }
    }
    
    return declaration + formatNode(rootElement)
  } catch (e) {
    throw new Error('XML格式化失败: ' + e.message)
  }
}

// 处理粘贴事件，自动格式化（仅 JSON）
const handlePaste = (e) => {
  // 只对 JSON 进行自动格式化，XML 不自动格式化
  if (sourceProtocol.value !== 'JSON') return
  
  // 使用 setTimeout 确保粘贴内容已经插入到输入框
  setTimeout(() => {
    const pastedText = sourceJson.value
    if (!pastedText || !pastedText.trim()) return
    
    try {
      // 尝试解析并格式化 JSON
      const parsed = JSON.parse(pastedText)
      sourceJson.value = JSON.stringify(parsed, null, 2)
      ElMessage.success('已自动格式化 JSON')
      // 格式化后重新解析
      parseSourceTree()
    } catch (e) {
      // 格式化失败时不提示，让用户手动格式化
      // 这样可以避免粘贴正常内容时弹出错误提示
    }
  }, 10)
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

// --- 搜索过滤逻辑 ---
/**
 * 递归过滤树节点
 * @param {Array} nodes - 节点数组
 * @param {string} keyword - 搜索关键词
 * @returns {Array} 过滤后的节点数组
 */
const filterTreeNodes = (nodes, keyword) => {
  if (!keyword || !keyword.trim()) {
    return nodes
  }
  
  const lowerKeyword = keyword.toLowerCase().trim()
  
  const filterNode = (node) => {
    // 检查当前节点是否匹配
    const labelMatch = node.label && node.label.toLowerCase().includes(lowerKeyword)
    const pathMatch = node.path && node.path.toLowerCase().includes(lowerKeyword)
    
    // 递归过滤子节点
    let filteredChildren = []
    if (node.children && node.children.length > 0) {
      filteredChildren = node.children
        .map(filterNode)
        .filter(child => child !== null)
    }
    
    // 如果当前节点匹配，或者有子节点匹配，则保留该节点
    if (labelMatch || pathMatch || filteredChildren.length > 0) {
      return {
        ...node,
        children: filteredChildren.length > 0 ? filteredChildren : node.children
      }
    }
    
    return null
  }
  
  return nodes
    .map(filterNode)
    .filter(node => node !== null)
}

// 过滤后的源数据树
const filteredSourceTreeData = computed(() => {
  if (!searchKeyword.value || !searchKeyword.value.trim()) {
    return sourceTreeData.value
  }
  
  if (!sourceTreeData.value || sourceTreeData.value.length === 0) {
    return []
  }
  
  // 对每个根节点进行过滤
  return sourceTreeData.value.map(rootNode => {
    if (!rootNode.children || rootNode.children.length === 0) {
      return rootNode
    }
    
    const filteredChildren = filterTreeNodes(rootNode.children, searchKeyword.value)
    return {
      ...rootNode,
      children: filteredChildren
    }
  })
})

// 搜索画布元素
const handleSearch = () => {
  if (!graph) return
  
  const keyword = searchKeyword.value?.trim()
  
  // 清除之前的高亮
  const allNodes = graph.getNodes()
  allNodes.forEach(node => {
    const attrs = node.getAttrs()
    const data = node.getData()
    // 恢复原始样式
    if (data?.type === 'source') {
      node.setAttrs({
        body: {
          fill: '#e3f2fd',
          stroke: '#2196f3',
          strokeWidth: 1,
          rx: 4
        }
      })
    } else if (data?.type === 'target') {
      node.setAttrs({
        body: {
          fill: '#f3e5f5',
          stroke: '#9c27b0',
          strokeWidth: 1,
          rx: 4
        }
      })
    }
  })
  
  // 如果没有搜索关键词，不进行高亮
  if (!keyword) {
    return
  }
  
  const lowerKeyword = keyword.toLowerCase()
  const matchedNodes = []
  
  // 搜索所有节点
  allNodes.forEach(node => {
    const data = node.getData()
    const label = node.attr('text/text') || ''
    const path = data?.path || ''
    
    // 检查是否匹配
    const labelMatch = label.toLowerCase().includes(lowerKeyword)
    const pathMatch = path.toLowerCase().includes(lowerKeyword)
    
    if (labelMatch || pathMatch) {
      matchedNodes.push(node)
      
      // 高亮显示匹配的节点（红色边框）
      const attrs = node.getAttrs()
      node.setAttrs({
        body: {
          ...attrs.body,
          stroke: '#ff4d4f', // 红色高亮
          strokeWidth: 3
        }
      })
    }
  })
  
  // 如果有匹配的节点，自动调整视野以显示所有匹配的节点
  if (matchedNodes.length > 0) {
    nextTick(() => {
      // 计算所有匹配节点的边界框
      let minX = Infinity
      let minY = Infinity
      let maxX = -Infinity
      let maxY = -Infinity
      
      matchedNodes.forEach(node => {
        const bbox = node.getBBox()
        minX = Math.min(minX, bbox.x)
        minY = Math.min(minY, bbox.y)
        maxX = Math.max(maxX, bbox.x + bbox.width)
        maxY = Math.max(maxY, bbox.y + bbox.height)
      })
      
      // 添加一些边距
      const padding = 50
      minX -= padding
      minY -= padding
      maxX += padding
      maxY += padding
      
      // 计算中心点和尺寸
      const centerX = (minX + maxX) / 2
      const centerY = (minY + maxY) / 2
      const width = maxX - minX
      const height = maxY - minY
      
      // 将视图中心移动到匹配节点的中心
      graph.centerPoint(centerX, centerY)
      
      // 如果匹配节点较多，自动缩放以适应视野
      if (matchedNodes.length > 1) {
        const containerRect = graphContainer.value.getBoundingClientRect()
        const scaleX = containerRect.width / width
        const scaleY = containerRect.height / height
        const scale = Math.min(scaleX, scaleY, 1) // 不超过原始大小
        
        if (scale < 1) {
          graph.zoomToFit({
            padding: 40,
            maxScale: 1,
            minScale: scale,
            preserveAspectRatio: true
          })
        }
      }
    })
  }
}

// 已映射的路径集合（用于实时更新标记）
const mappedPathsSet = ref(new Set())

// 更新已映射路径集合
const updateMappedPaths = () => {
  if (!graph) {
    mappedPathsSet.value = new Set()
    return
  }
  
  const paths = new Set()
  const nodes = graph.getNodes()
  
  nodes.forEach(node => {
    const nodeData = node.getData()
    if (nodeData?.type === 'source' && nodeData.path) {
      const path = nodeData.path
      // 添加原始路径
      paths.add(path)
      // 添加标准化路径（移除 $ 前缀）
      const normalizedPath = path.replace(/^\$\.?/, '')
      if (normalizedPath) {
        paths.add(normalizedPath)
        paths.add('$.' + normalizedPath)
      }
    }
  })
  
  mappedPathsSet.value = paths
}

// 检查字段是否已映射（在画布上有对应的源节点）
const isFieldMapped = (path) => {
  if (!path || mappedPathsSet.value.size === 0) return false
  
  // 标准化路径
  const normalizedPath = path.replace(/^\$\.?/, '')
  
  // 检查各种可能的路径格式
  return mappedPathsSet.value.has(path) ||
         mappedPathsSet.value.has(normalizedPath) ||
         mappedPathsSet.value.has('$.' + normalizedPath) ||
         mappedPathsSet.value.has('$' + normalizedPath)
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
  nextTick(() => {
    updatePreview()
    updateMappedPaths() // 更新映射状态
    nodeCount.value += 2 // 增加节点计数（创建了一对节点）
  })
}

/**
 * 递归收集树中所有叶子节点（字段）
 */
const collectAllFields = (node, fields = []) => {
  // 如果是叶子节点（没有children或children为空），且不是根节点
  if ((!node.children || node.children.length === 0) && node.path && node.path !== '$') {
    fields.push({
      label: node.label,
      path: node.path,
      type: node.type
    })
  } else if (node.children && node.children.length > 0) {
    // 递归处理子节点
    node.children.forEach(child => {
      collectAllFields(child, fields)
    })
  }
  return fields
}

/**
 * 自动生成映射规则
 */
const generateAutoMapping = () => {
  if (!graph || !sourceTreeData.value || sourceTreeData.value.length === 0) {
    return
  }
  
  // 收集所有字段
  const allFields = []
  sourceTreeData.value.forEach(rootNode => {
    if (rootNode.children) {
      rootNode.children.forEach(child => {
        collectAllFields(child, allFields)
      })
    }
  })
  
  if (allFields.length === 0) {
    ElMessage.warning('未找到可映射的字段')
    return
  }
  
  // 如果画布已有内容，询问是否清空
  if (nodeCount.value > 0) {
    ElMessageBox.confirm(
      '画布已有内容，自动映射将清空现有内容并重新生成，是否继续？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      // 清空画布
      graph.clearCells()
      nodeCount.value = 0
      nodeCounter = 0
      
      // 生成映射
      doGenerateAutoMapping(allFields)
    }).catch(() => {
      // 用户取消，关闭自动映射模式
      autoMappingMode.value = false
    })
  } else {
    // 直接生成映射
    doGenerateAutoMapping(allFields)
  }
}

/**
 * 执行自动映射生成
 */
const doGenerateAutoMapping = (fields) => {
  if (!graph) return
  
  // 优化布局参数：每行只放1个节点对，确保不重叠
  const nodeWidth = 140
  const nodeHeight = 40
  const sourceX = 100 // 源节点起始X坐标
  const targetX = 400 // 目标节点起始X坐标
  const startY = 100 // 起始Y坐标
  const rowHeight = 100 // 行高（节点高度40 + 间距60）
  const horizontalGap = 100 // 源节点和目标节点之间的水平间距
  
  fields.forEach((field, index) => {
    const y = startY + index * rowHeight
    
    // 提取字段名（从路径中获取最后一部分）
    const pathParts = field.path.split('.')
    const fieldName = pathParts[pathParts.length - 1].replace(/\[.*?\]/g, '') // 移除数组索引
    
    // 创建节点对
    const sId = `s_${++nodeCounter}`
    const tId = `t_${++nodeCounter}`
    
    // 1. 源节点（左侧）
    graph.addNode({
      id: sId,
      x: sourceX,
      y: y,
      width: nodeWidth,
      height: nodeHeight,
      label: fieldName,
      data: { type: 'source', path: field.path },
      ports: {
        groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
        items: [{ id: 'p1', group: 'right' }]
      },
      attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: fieldName, fontSize: 12 } }
    })
    
    // 2. 目标节点（右侧）
    graph.addNode({
      id: tId,
      x: targetX,
      y: y,
      width: nodeWidth,
      height: nodeHeight,
      label: fieldName,
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
    
    nodeCount.value += 2 // 增加节点计数
  })
  
  // 更新映射状态和预览
  nextTick(() => {
    updateMappedPaths()
    updatePreview()
    zoomToFit() // 自动适应视野
    ElMessage.success(`已自动生成 ${fields.length} 个映射规则`)
  })
}

/**
 * 自动映射模式切换
 */
const onAutoMappingModeChange = (value) => {
  if (value && sourceTreeData.value && sourceTreeData.value.length > 0 && sourceProtocol.value === 'JSON') {
    // 如果开启自动映射且已有解析的JSON数据，立即生成映射
    generateAutoMapping()
  }
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
 * 将对象转换为XML字符串
 */
const objectToXml = (obj, rootName = 'root') => {
  const escapeXml = (str) => {
    if (typeof str !== 'string') return String(str)
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;')
  }
  
  const formatValue = (value, indent = '') => {
    if (value === null || value === undefined) {
      return ''
    } else if (Array.isArray(value)) {
      // 数组：每个元素使用相同的标签名
      return value.map(item => {
        const formatted = formatValue(item, indent + '  ')
        // 如果数组元素是对象，需要找到合适的标签名
        // 这里简化处理，使用 'item' 作为标签名
        if (typeof item === 'object' && item !== null) {
          return `${indent}  <item>\n${formatted}\n${indent}  </item>`
        } else {
          return `${indent}  <item>${escapeXml(item)}</item>`
        }
      }).join('\n')
    } else if (typeof value === 'object') {
      const entries = Object.entries(value)
      if (entries.length === 0) {
        return ''
      }
      return entries.map(([key, val]) => {
        const nextIndent = indent + '  '
        const formattedVal = formatValue(val, nextIndent)
        if (formattedVal === '') {
          return `${indent}<${key}></${key}>`
        } else if (typeof val === 'object' && val !== null && !Array.isArray(val)) {
          return `${indent}<${key}>\n${formattedVal}\n${indent}</${key}>`
        } else {
          return `${indent}<${key}>${escapeXml(formattedVal)}</${key}>`
        }
      }).join('\n')
    } else {
      return escapeXml(String(value))
    }
  }
  
  const formatted = formatValue(obj, '  ')
  return `<?xml version="1.0" encoding="UTF-8"?>\n<${rootName}>\n${formatted}\n</${rootName}>`
}

/**
 * 添加字段到源数据
 */
const addFieldToSourceData = (fieldName) => {
  try {
    let currentData = {}
    let rootName = 'root'
    
    // 解析现有数据
    if (sourceJson.value.trim()) {
      if (sourceProtocol.value === 'XML') {
        // 提取根元素名称（跳过XML声明）
        const xmlContent = sourceJson.value.trim()
        const rootMatch = xmlContent.match(/<([a-zA-Z_][a-zA-Z0-9_-]*)[\s>]/)
        if (rootMatch && rootMatch[1] && rootMatch[1] !== 'xml') {
          rootName = rootMatch[1]
        }
        currentData = parseXmlToObject(sourceJson.value)
      } else {
        // JSON模式：从Monaco Editor获取内容，如果没有则从sourceJson获取
        let jsonText = sourceJson.value
        if (sourceJsonEditor) {
          jsonText = sourceJsonEditor.getValue()
        }
        if (jsonText.trim()) {
          currentData = JSON.parse(jsonText)
        }
      }
    }
    
    // 如果当前数据是数组，转换为对象
    if (Array.isArray(currentData)) {
      currentData = { items: currentData }
    }
    
    // 确保是对象
    if (typeof currentData !== 'object' || currentData === null) {
      currentData = {}
    }
    
    // 检查字段是否已存在，如果存在则不重复添加
    if (currentData[fieldName] !== undefined) {
      return
    }
    
    // 添加新字段，使用默认测试值
    currentData[fieldName] = `test_${fieldName}`
    
    // 根据协议类型格式化并更新
    let newContent = ''
    if (sourceProtocol.value === 'XML') {
      // XML格式：使用提取的或默认的根元素名称
      newContent = objectToXml(currentData, rootName)
      sourceJson.value = newContent
    } else {
      // JSON格式：格式化输出
      newContent = JSON.stringify(currentData, null, 2)
      sourceJson.value = newContent
      
      // 如果使用Monaco Editor，同步更新编辑器内容
      if (sourceJsonEditor) {
        sourceJsonEditor.setValue(newContent)
        // 格式化文档
        nextTick(() => {
          sourceJsonEditor.getAction('editor.action.formatDocument').run()
        })
      }
    }
    
    // 刷新树显示
    parseSourceTree()
  } catch (e) {
    console.error('添加字段到源数据失败:', e)
    // 如果解析失败，创建新的数据结构
    let newContent = ''
    if (sourceProtocol.value === 'XML') {
      newContent = `<?xml version="1.0" encoding="UTF-8"?>\n<root>\n  <${fieldName}>test_${fieldName}</${fieldName}>\n</root>`
      sourceJson.value = newContent
    } else {
      newContent = JSON.stringify({ [fieldName]: `test_${fieldName}` }, null, 2)
      sourceJson.value = newContent
      
      // 如果使用Monaco Editor，同步更新编辑器内容
      if (sourceJsonEditor) {
        sourceJsonEditor.setValue(newContent)
        // 格式化文档
        nextTick(() => {
          sourceJsonEditor.getAction('editor.action.formatDocument').run()
        })
      }
    }
    parseSourceTree()
  }
}

/**
 * 打开添加节点对话框
 */
const addNodePair = () => {
  if (!graph || !graphContainer.value) return
  
  // 重置表单
  addNodeForm.value = {
    fieldName: '',
    sourcePath: '',
    targetFieldName: '',
    targetPath: '',
    mappingType: 'ONE_TO_ONE',
    transformType: 'DIRECT'
  }
  
  // 检查是否有源数据，如果有则从源数据中提取未映射的字段作为默认值
  if (sourceTreeData.value && sourceTreeData.value.length > 0) {
    // 收集所有字段
    const allFields = []
    sourceTreeData.value.forEach(rootNode => {
      if (rootNode.children) {
        rootNode.children.forEach(child => {
          collectAllFields(child, allFields)
        })
      }
    })
    
    // 获取已映射的路径
    const mappedPaths = new Set()
    if (graph) {
      const cells = graph.getCells()
      cells.forEach(cell => {
        if (cell.isNode() && cell.getData()?.type === 'source' && cell.getData()?.path) {
          mappedPaths.add(cell.getData().path)
        }
      })
    }
    
    // 找到第一个未映射的字段，作为默认值
    const unmappedField = allFields.find(field => !mappedPaths.has(field.path))
    
    if (unmappedField) {
      // 提取字段名（从路径中获取最后一部分）
      const pathParts = unmappedField.path.split('.')
      const fieldName = pathParts[pathParts.length - 1].replace(/\[.*?\]/g, '') // 移除数组索引
      addNodeForm.value.fieldName = fieldName
      addNodeForm.value.sourcePath = unmappedField.path
      // 默认输出字段名与输入字段名相同
      addNodeForm.value.targetFieldName = fieldName
    } else {
      // 所有字段都已映射，使用默认字段名
      const defaultFieldName = `field_${nodeCounter + 1}`
      addNodeForm.value.fieldName = defaultFieldName
      addNodeForm.value.targetFieldName = defaultFieldName
    }
  } else {
    // 没有源数据，使用默认字段名
    const defaultFieldName = `field_${nodeCounter + 1}`
    addNodeForm.value.fieldName = defaultFieldName
    addNodeForm.value.targetFieldName = defaultFieldName
  }
  
  // 打开对话框
  addNodeDialogVisible.value = true
}

/**
 * 确认添加节点
 */
const confirmAddNode = () => {
  if (!addNodeForm.value.fieldName || !addNodeForm.value.fieldName.trim()) {
    ElMessage.warning('请输入输入字段名')
    return
  }
  
  if (!addNodeForm.value.targetFieldName || !addNodeForm.value.targetFieldName.trim()) {
    ElMessage.warning('请输入输出字段名')
    return
  }
  
  if (!graph || !graphContainer.value) return
  
  // 智能排版算法：每 10 个换一列
  const ROW_HEIGHT = 60
  const COLUMN_WIDTH = 450
  const row = autoLayoutCount % 10
  const col = Math.floor(autoLayoutCount / 10)
  
  // 基础起始点：x=250 (留出源节点空间), y=50
  const targetX = 250 + (col * COLUMN_WIDTH)
  const targetY = 50 + (row * ROW_HEIGHT)
  
  const sourceFieldName = addNodeForm.value.fieldName.trim()
  const sourcePath = addNodeForm.value.sourcePath.trim() || sourceFieldName
  const targetFieldName = addNodeForm.value.targetFieldName.trim()
  const targetPath = addNodeForm.value.targetPath.trim() || targetFieldName
  
  // 创建节点对
  const sId = `s_${++nodeCounter}`
  const tId = `t_${++nodeCounter}`
  
  // 1. 源节点（输入）
  graph.addNode({
    id: sId,
    x: targetX - 200,
    y: targetY - 25,
    width: 140,
    height: 40,
    label: sourceFieldName,
    data: { type: 'source', path: sourcePath },
    ports: {
      groups: { right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#2196f3', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'right' }]
    },
    attrs: { body: { fill: '#e3f2fd', stroke: '#2196f3', rx: 4 }, text: { text: sourceFieldName, fontSize: 12 } }
  })
  
  // 2. 目标节点（输出）
  graph.addNode({
    id: tId,
    x: targetX + 50,
    y: targetY - 25,
    width: 140,
    height: 40,
    label: targetFieldName,
    data: { type: 'target', path: targetPath },
    ports: {
      groups: { left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#9c27b0', fill: '#fff' } } } },
      items: [{ id: 'p1', group: 'left' }]
    },
    attrs: { body: { fill: '#f3e5f5', stroke: '#9c27b0', rx: 4 }, text: { text: targetFieldName, fontSize: 12 } }
  })
  
  // 3. 连线（使用配置的映射规则和转换类型）
  graph.addEdge({
    source: { cell: sId, port: 'p1' },
    target: { cell: tId, port: 'p1' },
    attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
    data: {
      mappingType: addNodeForm.value.mappingType,
      transformType: addNodeForm.value.transformType,
      transformConfig: {}
    },
    labels: [{ attrs: { text: { text: addNodeForm.value.transformType, fontSize: 10 } } }]
  })
  
  // 确保源数据中有这个字段（如果源数据为空或字段不存在，则添加）
  addFieldToSourceData(sourceFieldName)
  
  // 计数器加1，确保下次不重叠
  autoLayoutCount++
  nodeCount.value += 2
  
  // 更新映射状态
  updateMappedPaths()
  updatePreview()
  
  // 关闭对话框
  addNodeDialogVisible.value = false
  
  ElMessage.success('已添加新节点')
}

// 一键自适应视野
const zoomToFit = () => {
  if (!graph) return
  
  graph.zoomToFit({
    padding: 40,        // 画布四周留白，防止节点紧贴边缘
    maxScale: 1,        // 最大缩放级别为 1，防止少量节点时被放得太大
    minScale: 0.2,      // 最小缩放级别，防止节点太多时缩得太小看不清
    preserveAspectRatio: true // 保持纵横比
  })
  
  // 缩放后自动居中
  graph.centerContent()
  
  ElMessage({
    message: '视野已自动调整',
    type: 'success',
    duration: 1000
  })
}

// --- 右键菜单处理函数 ---
const handleContextMenuEdit = () => {
  if (!contextMenuTarget) return
  
  if (contextMenuType.value === 'node') {
    // 编辑节点
    openNodeEditDialog(contextMenuTarget)
  } else if (contextMenuType.value === 'edge') {
    // 编辑边（规则）
    openEdgeConfig(contextMenuTarget)
  }
  
  contextMenuVisible.value = false
}

const handleContextMenuDelete = () => {
  if (!contextMenuTarget || !graph) return
  
  if (contextMenuType.value === 'node') {
    // 删除节点（会同时删除相关的边）
    graph.removeCells([contextMenuTarget])
    updateMappedPaths()
    nextTick(() => updatePreview())
  } else if (contextMenuType.value === 'edge') {
    // 删除边
    graph.removeCells([contextMenuTarget])
    nextTick(() => updatePreview())
  }
  
  contextMenuVisible.value = false
}

// 监听点击事件，关闭右键菜单
const handleDocumentClick = (e) => {
  if (contextMenuVisible.value) {
    contextMenuVisible.value = false
  }
}

// 放大画布
const zoomIn = () => {
  if (!graph) return
  graph.zoom(0.2) // 放大 20%
}

// 缩小画布
const zoomOut = () => {
  if (!graph) return
  graph.zoom(-0.2) // 缩小 20%
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
  
  // 初始化字典配置（从edge data中读取dictionaryId和dictionaryDirection）
  let dictionaryId = null
  let dictionaryDirection = false
  
  if (data.transformType === 'DICTIONARY') {
    // 兼容旧版本：如果transformConfig中有dictionary，说明是旧配置
    const oldDict = data.transformConfig?.dictionary
    if (!oldDict) {
      // 新配置：从edge data中读取
      // 确保ID类型一致（可能是字符串或数字）
      const id = data.dictionaryId
      dictionaryId = id != null ? (typeof id === 'number' ? id : Number(id)) : null
      dictionaryDirection = data.dictionaryDirection !== undefined ? data.dictionaryDirection : false
    }
  }
  
  currentEdgeConfig.value = {
    sourcePath: s.getData().path,
    targetPath: t.getData().path,
    mappingType: data.mappingType || 'ONE_TO_ONE',
    transformType: data.transformType || 'DIRECT',
    transformConfig: data.transformConfig || {},
    dictionaryId: dictionaryId,
    dictionaryDirection: dictionaryDirection
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
  // 这里删掉 initGroovyEditor 的手动调用！交给上面的 @opened 事件
}

let currentEdge = null
const onTransformTypeChange = () => {
  // 切换转换类型时，清空旧的配置
  currentEdgeConfig.value.transformConfig = {}
  if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    // 初始化字典配置
    currentEdgeConfig.value.dictionaryId = null
    currentEdgeConfig.value.dictionaryDirection = false
  } else {
    currentEdgeConfig.value.dictionaryId = null
    currentEdgeConfig.value.dictionaryDirection = false
  }
  
}

const onDictionaryChange = () => {
  // 字典变化时的处理（可以在这里做一些验证）
  console.log('字典已选择:', currentEdgeConfig.value.dictionaryId)
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

// 加载字典列表
const loadDictionaryList = async () => {
  try {
    const res = await dictionaryApi.getAllDictionaries()
    if (res.data.success) {
      dictionaryList.value = res.data.data || []
    } else {
      console.error('加载字典列表失败:', res.data.errorMessage)
    }
  } catch (e) {
    console.error('加载字典列表失败:', e)
  }
}

// 加载可用函数列表
const loadAvailableFunctions = async () => {
  try {
    const res = await functionApi.getAvailableFunctions()
    if (res.data) {
      availableFunctions.value = res.data
    }
  } catch (e) {
    console.error('加载函数列表失败:', e)
  }
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
    // Groovy脚本：从编辑器获取内容
    if (groovyEditor) {
      config.groovyScript = groovyEditor.getValue() || 'return input'
    } else if (!config.groovyScript) {
      config.groovyScript = 'return input'
    }
  } else if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    // 字典映射：不再保存字典内容，只保存字典ID和方向到edge data
    // 兼容旧版本：如果dictKeys/dictValues有值，说明是旧配置，这里不处理，让用户重新选择字典
    // 新版本：使用dictionaryId和dictionaryDirection（在edge data中保存）
    if (!currentEdgeConfig.value.dictionaryId) {
      ElMessage.warning('请先选择字典')
      return
    }
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
  const edgeData = {
    ...currentEdge.getData(),
    mappingType: currentEdgeConfig.value.mappingType,
    transformType: currentEdgeConfig.value.transformType,
    transformConfig: config
  }
  
  // 如果是字典类型，添加字典ID和方向
  if (currentEdgeConfig.value.transformType === 'DICTIONARY') {
    edgeData.dictionaryId = currentEdgeConfig.value.dictionaryId
    edgeData.dictionaryDirection = currentEdgeConfig.value.dictionaryDirection !== undefined 
      ? currentEdgeConfig.value.dictionaryDirection 
      : false
  }
  
  currentEdge.setData(edgeData)
  
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
    
    // 更新预览编辑器内容
    updatePreviewEditor()
  } catch (e) {
    const errorMsg = e.response?.data?.errorMessage || e.message || '未知错误'
    previewError.value = '转换失败: ' + errorMsg
    console.error('转换失败详情:', e.response?.data || e)
    
    // 即使出错也更新编辑器（显示错误信息或空内容）
    if (previewResultEditor) {
      previewResultEditor.setValue('')
    }
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
      
      // 如果是字典类型，添加字典ID和方向
      if (edgeData.transformType === 'DICTIONARY') {
        if (edgeData.dictionaryId) {
          rule.dictionaryId = edgeData.dictionaryId
          rule.dictionaryDirection = edgeData.dictionaryDirection !== undefined ? edgeData.dictionaryDirection : false
        }
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
      nodeCount.value = 0
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
  
  // 更新节点数量
  nodeCount.value = graph.getNodes().length
  
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
          const edgeData = {
            mappingType: rule.mappingType || 'ONE_TO_ONE',
            transformType: rule.transformType || 'DIRECT',
            transformConfig: rule.transformConfig || {}
          }
          
          // 如果是字典类型，添加字典ID和方向
          if (rule.transformType === 'DICTIONARY' && rule.dictionaryId) {
            edgeData.dictionaryId = rule.dictionaryId
            edgeData.dictionaryDirection = rule.dictionaryDirection !== undefined ? rule.dictionaryDirection : false
          }
          
          graph.addEdge({
            source: { cell: sourceNodeId, port: 'p1' },
            target: { cell: targetNodeId, port: 'p1' },
            attrs: { line: { stroke: '#8f8f8f', strokeWidth: 2 } },
            data: edgeData,
            labels: [{ attrs: { text: { text: rule.transformType || 'DIRECT', fontSize: 10 } } }]
          })
        }
      }
    }
  })
  
  // 更新布局计数器，确保后续手动添加节点时从正确位置继续
  autoLayoutCount = currentRow
  
  // 更新映射状态
  updateMappedPaths()
}
</script>

<style scoped>
.canvas-editor { 
  padding: 0; 
  margin: 0;
  height: 100vh;
  overflow: hidden;
}
.editor-card { 
  height: 100vh; 
  margin: 0;
  border: none;
  border-radius: 0;
}
:deep(.el-card__header) {
  padding: 0 !important;
  margin: 0 !important;
  border: none !important;
}
:deep(.el-card__body) {
  padding: 0 !important;
  margin: 0 !important;
  height: calc(100vh - 60px);
}
.card-header {
  padding: 15px 20px;
  margin: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.editor-container { 
  display: flex; 
  gap: 0;
  height: 100%;
  margin: 0;
  padding: 0;
}
.source-panel { 
  display: flex; 
  flex-direction: column; 
  border: none;
  border-right: 1px solid #ddd;
  border-radius: 0;
  background-color: #fff;
  overflow: hidden;
  flex-shrink: 0;
  transition: width 0.2s ease;
}
.source-panel.collapsed {
  width: 50px !important;
}
.source-panel.collapsed .panel-header {
  justify-content: center;
  padding: 10px 5px;
}
.canvas-panel { 
  flex: 1; 
  min-width: 0; /* 必须有，否则 flex 不会自动收缩 */
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 极其重要 */
  position: relative; /* 为子元素提供基准 */
  border: none;
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
  border-radius: 0; 
}
.canvas-toolbar { 
  padding: 10px; 
  border-bottom: 1px solid #ddd; 
  background-color: #fff; 
  flex-shrink: 0; /* 固定高度，不参与 flex 伸缩 */
  height: 40px; /* 明确高度 */
  display: flex;
  gap: 10px; /* 按钮之间的间距 */
  align-items: center;
}
.graph-container { 
  width: 100% !important;
  height: 100% !important;
  position: absolute; /* 脱离 flex 文档流，防止撑开父容器 */
  top: 40px; /* 留出 canvas-toolbar 的高度 */
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #fafafa; 
  overflow: hidden;
}
.canvas-empty-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 14px;
  pointer-events: none;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
}
.preview-panel { 
  display: flex; 
  flex-direction: column; 
  border: none;
  border-left: 1px solid #ddd;
  border-radius: 0;
  background-color: #fff;
  overflow: hidden;
  flex-shrink: 0;
  transition: width 0.2s ease;
}
.preview-panel.collapsed {
  width: 50px !important;
}
.preview-panel.collapsed .panel-header {
  justify-content: center;
  padding: 10px 5px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}
.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}
.collapsed .panel-header h3 {
  display: none;
}
.collapse-btn {
  padding: 4px;
  font-size: 16px;
}
.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 15px;
  overflow-y: auto;
}
.data-tree { flex: 1; overflow: auto; border: 1px solid #ddd; padding: 10px; }
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}
.mapped-icon {
  color: #67c23a;
  font-size: 14px;
  margin-left: 4px;
}
.dict-item { display: flex; align-items: center; margin-bottom: 8px; }

:deep(.x6-node-selected) rect {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}
:deep(.x6-edge-selected) path {
  stroke: #ff4d4f !important;
  stroke-width: 2px !important;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 9999;
  min-width: 120px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: #f5f7fa;
}

.context-menu-item-danger {
  color: #f56c6c;
}

.context-menu-item-danger:hover {
  background-color: #fef0f0;
}

.context-menu-divider {
  height: 1px;
  background-color: #e4e7ed;
  margin: 4px 0;
}

/* Monaco Editor 样式 */
/* 强制 Monaco 的所有浮动组件（提示框、搜索框）处于最顶层 */
.monaco-editor .suggest-widget,
.monaco-editor .suggest-details,
.monaco-editor .context-view,
.editor-widget,
.editor-container,
.monaco-aria-container {
  z-index: 9999 !important;
}

/* 确保联想提示框的背景和边框可见 */
.monaco-editor .suggest-widget {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid #dcdfe6 !important;
}

/* 分隔条样式 */
.resizer {
  width: 4px;
  background-color: #e4e7ed;
  cursor: col-resize;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  transition: background-color 0.2s;
}

.resizer:hover {
  background-color: #409eff;
}

.resizer-left {
  border-right: 1px solid #ddd;
}

.resizer-right {
  border-left: 1px solid #ddd;
}

/* 输入框错误状态 */
.input-error :deep(.el-textarea__inner) {
  border-color: #f56c6c;
}

.input-error :deep(.el-textarea__inner):focus {
  border-color: #f56c6c;
}

/* 源数据内容区域布局 */
.source-content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.source-input-area {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 垂直分隔条 */
.resizer-vertical {
  width: 100%;
  height: 4px;
  background-color: #e4e7ed;
  cursor: row-resize;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  transition: background-color 0.2s;
  margin: 5px 0;
}

.resizer-vertical:hover {
  background-color: #409eff;
}
</style>

