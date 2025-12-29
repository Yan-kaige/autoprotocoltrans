<template>
  <el-container class="app-container">
    <el-aside :width="menuCollapsed ? '64px' : '200px'" class="app-aside" :class="{ 'collapsed': menuCollapsed }">
      <div class="app-header">
        <h1 v-show="!menuCollapsed">报文转换</h1>
        <el-button
          text
          :icon="menuCollapsed ? ArrowRight : ArrowLeft"
          @click="menuCollapsed = !menuCollapsed"
          class="collapse-btn"
        />
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="app-menu"
        :collapse="menuCollapsed"
      >
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>配置管理</span>
        </el-menu-item>
        <el-menu-item index="/dictionary">
          <el-icon><Collection /></el-icon>
          <span>字典管理</span>
        </el-menu-item>
        <el-menu-item index="/function">
          <el-icon><Operation /></el-icon>
          <span>函数管理</span>
        </el-menu-item>
        <el-menu-item index="/standard-protocol">
          <el-icon><Document /></el-icon>
          <span>标准协议</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, Collection, Operation, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const menuCollapsed = ref(false) // 菜单折叠状态，默认展开

const activeMenu = computed(() => {
  // 根据当前路径匹配菜单项
  const path = route.path
  if (path.startsWith('/config') || path === '/') {
    return '/config'
  } else if (path.startsWith('/dictionary')) {
    return '/dictionary'
  } else   if (path.startsWith('/function')) {
    return '/function'
  } else if (path.startsWith('/standard-protocol')) {
    return '/standard-protocol'
  }
  return path
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  width: 100%;
  height: 100vh;
}

.app-container {
  height: 100vh;
}

.app-aside {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.app-aside.collapsed {
  width: 64px !important;
}

.app-header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  flex-shrink: 0;
}

.app-header h1 {
  font-size: 24px;
  font-weight: 500;
}

.app-aside.collapsed .app-header {
  justify-content: center;
  padding: 0 10px;
}

.collapse-btn {
  color: white;
  padding: 4px;
  font-size: 16px;
}

.app-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.app-main {
  background-color: #f5f7fa;
  padding: 0;
  overflow-y: auto;
}
</style>

