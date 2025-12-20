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
  // 获取所有可用函数
  getAvailableFunctions: () => api.get('/functions')
}

export default api

