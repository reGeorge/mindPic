# 02opt.md

## 安卓原生开发准备

1. **开发环境搭建** 已完成
   - 安装 [Android Studio](https://developer.android.com/studio)
   - 配置 JDK（推荐 JDK 11 或更高版本）
   - 配置 Android SDK 和必要的模拟器/真机调试环境
   - 熟悉 Gradle 构建工具

2. **项目初始化** 已完成
   - 新建 Android Studio 项目，选择 Empty Activity 模板
   - 配置包名、最小SDK版本（建议21及以上）
   - 配置多语言支持（如需）
   - 配置必要的依赖库（如 Jetpack、Glide、Room 等）

3. **资源准备** 已完成资源导入
   - 准备好 UI 设计稿（可参考 web 端实现）
   - 导入图片、字体等资源到 `res` 目录
   - 设计应用图标和启动页

---

## MVP功能实现思路

### 1. 架构设计
- 推荐采用 MVVM 或 MVP 架构，便于后续维护和扩展
- 使用 Jetpack 组件（ViewModel、LiveData、DataBinding 等）提升开发效率

### 2. 主要功能模块拆分
- **首页/Home**：展示核心内容，UI 参考 web 端
- **图片/画布处理**：如有自定义绘图或图片处理，可用 Canvas、Bitmap 等原生 API 实现
- **字体加载**：将自定义字体放入 `assets/fonts`，通过 Typeface 加载
- **数据存储**：如需本地存储，优先使用 Room 或 SharedPreferences

### 3. 关键实现点
- **自定义View/画布**：如 web 端有 canvas 逻辑，安卓可通过继承 View 并重写 onDraw 实现
- **资源适配**：图片、字体需适配不同分辨率和屏幕密度
- **权限处理**：如涉及存储、相机等功能，需动态申请权限
- **性能优化**：图片加载建议用 Glide/Picasso，避免 OOM

### 4. MVP功能优先级
- 仅实现最小可用功能（如首页展示、基础交互、核心画布/图片处理）
- 非核心功能（如分享、登录等）可后续迭代

---

## 参考资料
- [Android 官方文档](https://developer.android.com/)
- [Jetpack 组件介绍](https://developer.android.com/jetpack)
- [自定义View开发指南](https://developer.android.com/guide/topics/ui/custom-components) 