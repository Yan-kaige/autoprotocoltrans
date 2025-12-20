import { createRouter, createWebHistory } from 'vue-router'
import RuleList from '../views/RuleList.vue'
import RuleEdit from '../views/RuleEdit.vue'
import CanvasEditor from '../views/CanvasEditor.vue'

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
  },
  {
    path: '/canvas',
    name: 'CanvasEditor',
    component: CanvasEditor
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

