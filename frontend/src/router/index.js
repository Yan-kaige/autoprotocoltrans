import { createRouter, createWebHistory } from 'vue-router'
import RuleList from '../views/RuleList.vue'
import RuleEdit from '../views/RuleEdit.vue'
import CanvasEditor from '../views/CanvasEditor.vue'
import ConfigList from '../views/ConfigList.vue'
import DictionaryList from '../views/DictionaryList.vue'

const routes = [
  {
    path: '/',
    name: 'ConfigList',
    component: ConfigList
  },
  {
    path: '/config',
    name: 'ConfigListAlias',
    component: ConfigList
  },
  {
    path: '/canvas',
    name: 'CanvasEditor',
    component: CanvasEditor
  },
  {
    path: '/dictionary',
    name: 'DictionaryList',
    component: DictionaryList
  },
  {
    path: '/rule',
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

