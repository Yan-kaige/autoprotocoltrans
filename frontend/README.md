# 前端项目说明

## 安装依赖

```bash
npm install
```

## 开发运行

```bash
npm run dev
```

## 构建生产版本

```bash
npm run build
```

## 新增依赖说明

### AntV X6
- `@antv/x6`: 图编辑引擎核心库
- `@antv/x6-vue-shape`: Vue集成组件（当前版本可能不需要，保留以备后用）

### 已安装的其他依赖
- Vue 3
- Element Plus
- Vue Router
- Axios
- VueDraggable

## 页面路由

- `/` - 规则列表页
- `/rule/:id?` - 规则编辑页
- `/canvas` - 画布编辑器（新功能）

## 使用说明

详细使用说明请参考 `docs/CANVAS_USAGE.md`

