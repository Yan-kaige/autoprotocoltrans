import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 规则API
export const ruleApi = {
  // 获取所有规则
  getAllRules: () => api.get('/rules'),
  
  // 根据ID获取规则
  getRuleById: (id) => api.get(`/rules/${id}`),
  
  // 保存规则
  saveRule: (rule) => api.post('/rules', rule),
  
  // 删除规则
  deleteRule: (id) => api.delete(`/rules/${id}`),
  
  // 执行转换
  transform: (ruleId, sourceData, prettyPrint = false) => 
    api.post('/rules/transform', {
      ruleId,
      sourceData,
      prettyPrint
    })
}

// 函数API
export const functionApi = {
  // 获取所有可用函数（包括系统函数和自定义函数）
  getAvailableFunctions: () => api.get('/functions'),
  
  // 获取所有自定义函数列表
  getAllFunctions: () => api.get('/v2/function/list'),
  
  // 根据ID获取函数
  getFunction: (id) => api.get(`/v2/function/${id}`),
  
  // 保存函数（新增或更新）
  saveFunction: (id, name, code, description, script, enabled) =>
    api.post('/v2/function/save', {
      id: id || null,
      name,
      code,
      description,
      script,
      enabled
    }),
  
  // 删除函数
  deleteFunction: (id) => api.delete(`/v2/function/${id}`),
  
  // 检查编码是否存在
  checkCodeExists: (code, excludeId) => 
    api.get('/v2/function/check-code', {
      params: { code, excludeId }
    })
}

// V2版本转换API
export const transformV2Api = {
  // 执行转换（使用MappingConfig）
  transform: (sourceData, mappingConfig) => 
    api.post('/v2/transform', {
      sourceData,
      mappingConfig
    }),
  
  // 根据配置名称执行转换
  transformByName: (sourceData, configName) =>
    api.post('/v2/transform/by-name', {
      sourceData,
      configName
    })
}

// 配置管理API
export const configApi = {
  // 获取所有配置列表
  getAllConfigs: () => api.get('/v2/config/list'),
  
  // 根据ID获取配置
  getConfigById: (id) => api.get(`/v2/config/${id}`),
  
  // 根据名称获取配置
  getConfigByName: (name) => api.get(`/v2/config/name/${name}`),
  
  // 保存配置
  saveConfig: (id, name, description, bankCategory, transactionName, requestType, version, createNewVersion, mappingConfig) =>
    api.post('/v2/config/save', {
      id: id || null, // 如果为null则表示新建
      name,
      description,
      bankCategory,
      transactionName,
      requestType,
      version,
      createNewVersion, // 是否创建新版本
      mappingConfig
    }),
  
  // 获取标准交易类型列表
  getTransactionTypes: () => api.get('/v2/config/transaction-types'),
  
  // 删除配置
  deleteConfig: (id) => api.delete(`/v2/config/${id}`),
  
  // 检查配置名称是否存在
  checkNameExists: (name) => api.get(`/v2/config/check-name/${name}`),
  
  // 获取所有银行类别列表
  getAllBankCategories: () => api.get('/v2/config/bank-categories'),
  
  // 根据银行类别和交易类型查询配置
  getConfigsByBankAndTransaction: (bankCategory, requestType) =>
    api.get('/v2/config/by-bank-transaction', {
      params: { bankCategory, requestType }
    }),
  
  // 根据银行类别、交易类型和配置类型查询配置
  getConfigByBankTransactionAndType: (bankCategory, requestType, configType, version) =>
    api.get('/v2/config/by-bank-transaction-type', {
      params: { bankCategory, requestType, configType, version }
    }),
  
  // 获取指定银行和交易类型的所有版本列表
  getVersions: (bankCategory, requestType) =>
    api.get('/v2/config/versions', {
      params: { bankCategory, requestType }
    }),
  
  // 回退到指定版本
  rollbackToVersion: (bankCategory, requestType, version) =>
    api.post('/v2/config/rollback', {
      bankCategory,
      requestType,
      version
    }),
  
  // 统计指定银行的已配置请求数量
  countRequestConfigs: (bankCategory) => api.get(`/v2/config/count-requests/${bankCategory}`),
  
  // 切换配置的启用状态
  toggleEnabled: (id) => api.post(`/v2/config/toggle-enabled/${id}`)
}

// 字典管理API
export const dictionaryApi = {
  // 获取所有字典列表
  getAllDictionaries: () => api.get('/v2/dictionary/list'),
  
  // 根据ID获取字典及其项
  getDictionary: (id) => api.get(`/v2/dictionary/${id}`),
  
  // 保存字典（新增或更新）
  saveDictionary: (id, name, code, description, items) =>
    api.post('/v2/dictionary/save', {
      id: id || null,
      name,
      code,
      description,
      items
    }),
  
  // 删除字典
  deleteDictionary: (id) => api.delete(`/v2/dictionary/${id}`),
  
  // 检查编码是否存在
  checkCodeExists: (code, excludeId) => 
    api.get('/v2/dictionary/check-code', {
      params: { code, excludeId }
    })
}

// 银行信息管理API
export const bankApi = {
  // 获取所有银行列表
  getAllBanks: () => api.get('/v2/bank/list'),
  
  // 获取所有启用的银行列表
  getEnabledBanks: () => api.get('/v2/bank/list/enabled'),
  
  // 根据ID获取银行
  getBankById: (id) => api.get(`/v2/bank/${id}`),
  
  // 保存银行（新增或更新）
  saveBank: (id, name, code, description, enabled) =>
    api.post('/v2/bank/save', {
      id: id || null,
      name,
      code,
      description,
      enabled
    }),
  
  // 删除银行
  deleteBank: (id) => api.delete(`/v2/bank/${id}`),
  
  // 检查编码是否存在
  checkCodeExists: (code, excludeId) => 
    api.get('/v2/bank/check-code', {
      params: { code, excludeId }
    }),
  
  // 导出银行配置为插件包
  exportBankPlugin: (bankId) => 
    api.get(`/v2/bank/${bankId}/export-plugin`, {
      responseType: 'blob'
    })
}

// 交易类型管理API（新结构）
export const transactionTypeApi = {
  // 根据银行ID获取所有交易类型
  getByBankId: (bankId) => api.get(`/v2/transaction-type/by-bank/${bankId}`),
  
  // 根据ID获取交易类型
  getById: (id) => api.get(`/v2/transaction-type/${id}`),
  
  // 保存交易类型（新增或更新）
  saveTransactionType: (id, bankId, transactionName, description, enabled) =>
    api.post('/v2/transaction-type/save', {
      id: id || null,
      bankId,
      transactionName,
      description,
      enabled
    }),
  
  // 删除交易类型
  deleteTransactionType: (id) => api.delete(`/v2/transaction-type/${id}`)
}

// 配置管理API V2（新结构）
export const configV2Api = {
  // 保存配置
  saveConfig: (id, transactionTypeId, version, name, description, createNewVersion, mappingConfig) =>
    api.post('/v2/config/save', {
      id: id || null,
      transactionTypeId,
      version,
      name,
      description,
      createNewVersion,
      mappingConfig
    }),
  
  // 根据ID获取配置
  getConfigById: (id) => api.get(`/v2/config/${id}`),
  
  // 获取当前版本的配置
  getCurrentConfig: (transactionTypeId, configType) =>
    api.get('/v2/config/current', {
      params: { transactionTypeId, configType }
    }),
  
  // 获取指定版本的配置
  getConfigByVersion: (transactionTypeId, configType, version) =>
    api.get('/v2/config/by-version', {
      params: { transactionTypeId, configType, version }
    }),
  
  // 获取所有版本列表
  getVersions: (transactionTypeId) =>
    api.get('/v2/config/versions', {
      params: { transactionTypeId }
    }),
  
  // 回退到指定版本
  rollbackToVersion: (transactionTypeId, version) =>
    api.post('/v2/config/rollback', {
      transactionTypeId,
      version
    }),
  
  // 删除配置
  deleteConfig: (id) => api.delete(`/v2/config/${id}`),

  // 获取标准交易类型列表
  getTransactionTypes: () => api.get('/v2/config/transaction-types')
}

// 标准协议管理API
export const standardProtocolApi = {
  // 获取所有标准协议
  getAllProtocols: () => api.get('/standard-protocol'),
  
  // 根据协议类型获取启用的协议列表
  getProtocolsByType: (protocolType) => 
    api.get('/standard-protocol/by-type', {
      params: { protocolType }
    }),
  
  // 根据ID获取协议
  getProtocolById: (id) => api.get(`/standard-protocol/${id}`),
  
  // 保存协议（新增或更新）
  saveProtocol: (id, name, code, description, protocolType, dataFormat, category) =>
    api.post('/standard-protocol', {
      id: id || null,
      name,
      code,
      description,
      protocolType,
      dataFormat,
      category
    }),
  
  // 删除协议
  deleteProtocol: (id) => api.delete(`/standard-protocol/${id}`),
  
  // 切换启用状态
  toggleEnabled: (id) => api.post(`/standard-protocol/${id}/toggle-enabled`),
  
  // 从文档导入协议
  importFromDocument: (documentContent, file) => {
    const formData = new FormData()
    if (file) {
      formData.append('file', file)
    }
    if (documentContent) {
      formData.append('documentContent', documentContent)
    }
    return api.post('/standard-protocol/import-from-document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      // 文档导入需要调用 LLM API，可能需要较长时间，设置 60 秒超时
      timeout: 60000
    })
  }
}

export default api

