# MindPic - 手写体图片生成器

## 项目结构

```
mindPic/
├── src/                    # 源代码目录
│   ├── components/         # 可复用组件
│   │   ├── TextInput/     # 文本输入组件
│   │   ├── Preview/       # 预览组件
│   │   └── Buttons/       # 按钮组件
│   ├── screens/           # 页面组件
│   │   └── Home/         # 主页面
│   ├── assets/           # 静态资源
│   │   └── fonts/        # 字体文件目录
│   ├── utils/            # 工具函数
│   │   ├── fontLoader.js # 字体加载工具
│   │   └── canvas.js     # Canvas相关工具
│   ├── store/            # 状态管理
│   ├── hooks/            # 自定义Hooks
│   ├── services/         # 服务层
│   └── constants/        # 常量定义
├── docs/                 # 文档目录
│   ├── 00mvp.md         # MVP文档
│   └── 01prd.md         # PRD文档
├── package.json         # 项目依赖配置
└── README.md           # 项目说明文档
```

## 开发环境设置

1. 安装依赖
```bash
npm install
```

2. 启动开发服务器
```bash
npm start
```

## 字体文件说明

字体文件应放置在 `src/assets/fonts/` 目录下，支持的格式：
- TTF
- OTF

## 开发规范

1. 组件开发
   - 使用函数式组件
   - 使用TypeScript
   - 遵循React Native最佳实践

2. 状态管理
   - 使用Redux进行全局状态管理
   - 使用React Hooks进行组件状态管理

3. 样式管理
   - 使用StyleSheet创建样式
   - 遵循React Native样式规范 