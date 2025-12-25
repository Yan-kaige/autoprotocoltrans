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
  saveConfig: (id, name, description, mappingConfig) =>
    api.post('/v2/config/save', {
      id: id || null, // 如果为null则表示新建
      name,
      description,
      mappingConfig
    }),
  
  // 删除配置
  deleteConfig: (id) => api.delete(`/v2/config/${id}`),
  
  // 检查配置名称是否存在
  checkNameExists: (name) => api.get(`/v2/config/check-name/${name}`)
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

export default api

