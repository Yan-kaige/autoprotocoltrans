import { createRouter, createWebHistory } from 'vue-router'
import RuleList from '../views/RuleList.vue'
import RuleEdit from '../views/RuleEdit.vue'

const routes = [
  {
    path: '/',
    name: 'RuleList',
    component: RuleList
  },
  {
    path: '/rule/:id?',
    name: 'RuleEdit',
    component: RuleEdit
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

