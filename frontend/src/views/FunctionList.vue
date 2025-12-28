<template>
  <div class="function-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>函数管理</span>
          <el-button type="primary" @click="openFunctionDialog">新建函数</el-button>
        </div>
      </template>

      <el-table :data="functionList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="函数名称" />
        <el-table-column prop="code" label="函数编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editFunction(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteFunction(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 函数编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentFunctionId ? '编辑函数' : '新建函数'" 
      width="800px"
      :append-to-body="true"
      :trap-focus="false"
      :destroy-on-close="true"
      @opened="handleDialogOpened"
      @close="resetDialog"
    >
      <el-form :model="functionForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="函数名称" prop="name">
          <el-input v-model="functionForm.name" placeholder="请输入函数名称" />
        </el-form-item>
        <el-form-item label="函数编码" prop="code">
          <el-input v-model="functionForm.code" placeholder="请输入函数编码（唯一，用于调用）" />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            编码将作为函数调用时的名称，例如：myFunction
          </div>
        </el-form-item>
        <el-form-item label="函数描述">
          <el-input 
            v-model="functionForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入函数描述（可选）"
          />
        </el-form-item>
        <el-form-item label="Groovy脚本" prop="script">
          <div class="monaco-wrapper" :class="{ 'is-fullscreen': isEditorFullScreen }">
            <div class="monaco-toolbar" v-show="!isEditorFullScreen">
              <el-button 
                link 
                :icon="FullScreen" 
                @click="toggleEditorFullScreen"
              >
                全屏编辑
              </el-button>
            </div>
            <div 
              id="function-monaco-editor" 
              style="width: 100%; pointer-events: auto !important;"
            ></div>
          </div>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            脚本说明：输入值通过 <code>input</code> 变量访问，需要返回转换后的结果
          </div>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="functionForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFunction" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FullScreen } from '@element-plus/icons-vue'
import { functionApi } from '../api'
import loader from '@monaco-editor/loader'
const functionList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentFunctionId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const functionForm = ref({
  name: '',
  code: '',
  description: '',
  script: '',
  enabled: true
})

let functionEditor = null // Monaco Editor 实例
let isMonacoProviderRegistered = false // Monaco Provider 是否已注册

// Monaco 编辑器全屏状态
const isEditorFullScreen = ref(false)
let escKeyHandler = null // ESC 键监听器

const rules = {
  name: [{ required: true, message: '请输入函数名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入函数编码', trigger: 'blur' }],
  script: [{ required: true, message: '请输入Groovy脚本', trigger: 'blur' }]
}

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
    triggerCharacters: ['.'],
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

      if (textBeforeCursor.endsWith('input.')) {
        return {
          suggestions: [
            { label: 'toString()', kind: monaco.languages.CompletionItemKind.Method, documentation: '将值转换为字符串', insertText: 'toString()', range },
            { label: 'split()', kind: monaco.languages.CompletionItemKind.Method, documentation: '字符串分割：input.split("-")', insertText: 'split(\'${1:separator}\')', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
            { label: 'substring()', kind: monaco.languages.CompletionItemKind.Method, documentation: '截取子字符串：input.substring(0, 3)', insertText: 'substring(${1:start}, ${2:end})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
            { label: 'trim()', kind: monaco.languages.CompletionItemKind.Method, documentation: '去除首尾空格', insertText: 'trim()', range },
            { label: 'toLowerCase()', kind: monaco.languages.CompletionItemKind.Method, documentation: '转换为小写', insertText: 'toLowerCase()', range },
            { label: 'toUpperCase()', kind: monaco.languages.CompletionItemKind.Method, documentation: '转换为大写', insertText: 'toUpperCase()', range },
            { label: 'length', kind: monaco.languages.CompletionItemKind.Property, documentation: '获取字符串长度', insertText: 'length', range },
            { label: 'replace()', kind: monaco.languages.CompletionItemKind.Method, documentation: '字符串替换：input.replace("old", "new")', insertText: 'replace(${1:searchValue}, ${2:replaceValue})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range }
          ]
        }
      }
      
      if (textBeforeCursor.endsWith('inputs.')) {
        return {
          suggestions: [
            { label: 'length', kind: monaco.languages.CompletionItemKind.Property, documentation: '数组长度', insertText: 'length', range },
            { label: 'map()', kind: monaco.languages.CompletionItemKind.Method, documentation: '数组映射：inputs.map { it.toString() }', insertText: 'map(${1:callback})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
            { label: 'filter()', kind: monaco.languages.CompletionItemKind.Method, documentation: '数组过滤：inputs.filter { it != null }', insertText: 'filter(${1:callback})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
            { label: 'join()', kind: monaco.languages.CompletionItemKind.Method, documentation: '数组连接：inputs.join(",")', insertText: 'join(${1:separator})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
            { label: 'forEach()', kind: monaco.languages.CompletionItemKind.Method, documentation: '遍历数组：inputs.forEach { ... }', insertText: 'forEach(${1:callback})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range }
          ]
        }
      }
      
      return { suggestions: [] }
    }
  })
  
  isMonacoProviderRegistered = true
}

// 初始化函数编辑器（Monaco Editor）
const initFunctionEditor = async () => {
  if (functionEditor) {
    functionEditor.dispose()
    functionEditor = null
  }

  setTimeout(async () => {
    const domElement = document.getElementById('function-monaco-editor')
    if (!domElement) {
      console.warn('函数编辑器容器未找到')
      return
    }

    try {
      const monaco = await loader.init()
      
      // 执行注册逻辑（全局只注册一次）
      await registerMonacoProviders(monaco)
      
      functionEditor = monaco.editor.create(domElement, {
        value: functionForm.value.script || 'return input',
        language: 'javascript',
        theme: 'vs',
        automaticLayout: true,
        fixedOverflowWidgets: true,
        suggestOnTriggerCharacters: true,
        quickSuggestions: { other: true, comments: false, strings: true },
        acceptSuggestionOnEnter: 'on',
        parameterHints: { enabled: true },
        wordBasedSuggestions: true,
        lineNumbers: 'on',
        renderLineHighlight: 'all',
        selectOnLineNumbers: true,
        readOnly: false,
        accessibilitySupport: 'on'
      })

      functionEditor.onDidChangeModelContent(() => {
        if (functionEditor) {
          const val = functionEditor.getValue()
          functionForm.value.script = val
        }
      })

      domElement.addEventListener('mousedown', () => {
        if (functionEditor) {
          functionEditor.focus()
        }
      }, true)

      // 添加 F11 和 ESC 快捷键支持全屏切换
      functionEditor.addAction({
        id: 'toggle-fullscreen',
        label: 'Toggle Full Screen',
        keybindings: [monaco.KeyCode.F11],
        run: () => {
          toggleEditorFullScreen()
        }
      })
      
      // ESC 键退出全屏（只在全屏时生效）
      functionEditor.addAction({
        id: 'exit-fullscreen',
        label: 'Exit Full Screen',
        keybindings: [monaco.KeyCode.Escape],
        run: () => {
          if (isEditorFullScreen.value) {
            toggleEditorFullScreen()
          }
        }
      })

      functionEditor.layout()
      functionEditor.focus()
    } catch (e) {
      console.error("Monaco load error:", e)
    }
  }, 400)
}

// 切换编辑器全屏状态
const toggleEditorFullScreen = () => {
  isEditorFullScreen.value = !isEditorFullScreen.value
  
  // 全屏时添加 ESC 键监听，退出全屏时移除
  if (isEditorFullScreen.value) {
    escKeyHandler = (e) => {
      if (e.key === 'Escape' && isEditorFullScreen.value) {
        toggleEditorFullScreen()
      }
    }
    document.addEventListener('keydown', escKeyHandler)
  } else {
    if (escKeyHandler) {
      document.removeEventListener('keydown', escKeyHandler)
      escKeyHandler = null
    }
  }
  
  // 关键：Monaco 不会自动跟随 DOM 变化调整尺寸
  // 必须在 DOM 更新后的 nextTick 调用 layout()
  // 使用 setTimeout 确保 CSS 过渡完成后再重新布局
  nextTick(() => {
    setTimeout(() => {
      if (functionEditor) {
        // 强制重新计算尺寸
        const domElement = document.getElementById('function-monaco-editor')
        if (domElement) {
          // 先获取容器的实际尺寸
          const rect = domElement.getBoundingClientRect()
          // 调用 layout 重新计算
          functionEditor.layout({
            width: rect.width,
            height: rect.height
          })
          functionEditor.focus() // 切换后保持焦点，方便继续输入
        }
      }
    }, 50) // 给 CSS 过渡一点时间
  })
}

// 监听全屏状态变化，重新布局编辑器
watch(() => isEditorFullScreen.value, () => {
  nextTick(() => {
    setTimeout(() => {
      if (functionEditor) {
        const domElement = document.getElementById('function-monaco-editor')
        if (domElement) {
          const rect = domElement.getBoundingClientRect()
          functionEditor.layout({
            width: rect.width,
            height: rect.height
          })
          functionEditor.focus()
        }
      }
    }, 50)
  })
})

// 监听对话框关闭，清理全屏状态
watch(() => dialogVisible.value, (val) => {
  if (!val) {
    // 对话框关闭时强制退出全屏，防止死锁
    if (isEditorFullScreen.value) {
      isEditorFullScreen.value = false
      // 清理 ESC 键监听器
      if (escKeyHandler) {
        document.removeEventListener('keydown', escKeyHandler)
        escKeyHandler = null
      }
    }
  }
})

// 对话框打开后的回调
const handleDialogOpened = () => {
  nextTick(() => {
    // 如果编辑器已存在，更新内容；否则初始化
    if (functionEditor) {
      functionEditor.setValue(functionForm.value.script || 'return input')
      functionEditor.layout()
    } else {
      initFunctionEditor()
    }
  })
}


onMounted(() => {
  loadFunctionList()
})

onUnmounted(() => {
  if (functionEditor) {
    functionEditor.dispose()
    functionEditor = null
  }
})

const loadFunctionList = async () => {
  loading.value = true
  try {
    const res = await functionApi.getAllFunctions()
    if (res.data.success) {
      functionList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.errorMessage || '加载函数列表失败')
    }
  } catch (e) {
    console.error('加载函数列表失败:', e)
    ElMessage.error('加载函数列表失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const openFunctionDialog = () => {
  currentFunctionId.value = null
  resetDialog()
  dialogVisible.value = true
  // 编辑器初始化由 @opened 事件处理
}

const editFunction = async (id) => {
  try {
    const res = await functionApi.getFunction(id)
    if (res.data.success) {
      const data = res.data.data
      currentFunctionId.value = id
      functionForm.value = {
        name: data.name,
        code: data.code,
        description: data.description || '',
        script: data.script || '',
        enabled: data.enabled !== undefined ? data.enabled : true
      }
      dialogVisible.value = true
      // 编辑器内容更新由 @opened 事件处理
    } else {
      ElMessage.error(res.data.errorMessage || '加载函数失败')
    }
  } catch (e) {
    console.error('加载函数失败:', e)
    ElMessage.error('加载函数失败: ' + (e.message || '未知错误'))
  }
}

const resetDialog = () => {
  functionForm.value = {
    name: '',
    code: '',
    description: '',
    script: '',
    enabled: true
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
  // 退出全屏并清理监听器
  if (isEditorFullScreen.value) {
    isEditorFullScreen.value = false
    if (escKeyHandler) {
      document.removeEventListener('keydown', escKeyHandler)
      escKeyHandler = null
    }
  }
  // 销毁编辑器
  if (functionEditor) {
    functionEditor.dispose()
    functionEditor = null
  }
}

const saveFunction = async () => {
  if (!formRef.value) return
  
  // 从编辑器获取最新内容
  if (functionEditor) {
    functionForm.value.script = functionEditor.getValue()
  }
  
  try {
    await formRef.value.validate()
    
    saving.value = true
    const res = await functionApi.saveFunction(
      currentFunctionId.value,
      functionForm.value.name.trim(),
      functionForm.value.code.trim(),
      functionForm.value.description?.trim() || '',
      functionForm.value.script.trim(),
      functionForm.value.enabled
    )
    
    if (res.data.success) {
      ElMessage.success(currentFunctionId.value ? '函数更新成功' : '函数保存成功')
      dialogVisible.value = false
      loadFunctionList()
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

const deleteFunction = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该函数吗？删除后使用该函数的映射配置将无法正常工作。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await functionApi.deleteFunction(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      loadFunctionList()
    } else {
      ElMessage.error(res.data.errorMessage || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      const errorMsg = e.response?.data?.errorMessage || e.message || '删除失败'
      ElMessage.error(errorMsg)
    }
  }
}
</script>

<style scoped>
.function-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

code {
  background-color: #f4f4f5;
  color: #e6a23c;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

/* Monaco Editor 样式 */
.monaco-editor .suggest-widget,
.monaco-editor .suggest-details,
.monaco-editor .context-view,
.editor-widget,
.editor-container,
.monaco-aria-container {
  z-index: 9999 !important;
}

.monaco-editor .suggest-widget {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid #dcdfe6 !important;
}

/* Monaco 编辑器全屏样式 */
.monaco-wrapper {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
}

/* 内部工具栏，放置按钮 */
.monaco-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 4px 8px;
  flex-shrink: 0;
}

/* 非全屏状态：工具栏不显示背景和边框 */
.monaco-wrapper:not(.is-fullscreen) .monaco-toolbar {
  background: transparent;
  border: none;
  padding: 4px 0;
  margin-bottom: 4px;
}

/* 全屏状态：隐藏工具栏 */
.monaco-wrapper.is-fullscreen .monaco-toolbar {
  display: none;
}

/* 全屏状态样式 */
.monaco-wrapper.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 6000 !important; /* 必须高于 Dialog 和所有弹窗 */
  background: #fff;
}

.monaco-wrapper.is-fullscreen #function-monaco-editor {
  height: 100vh !important; /* 全屏时占满整个视口 */
  border-radius: 0;
  border: none;
}

/* 非全屏状态：确保编辑器恢复原始尺寸 */
.monaco-wrapper:not(.is-fullscreen) #function-monaco-editor {
  height: 400px !important;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
</style>


