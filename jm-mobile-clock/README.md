# JMcomic Clock

这是 Wear OS 手表端应用模块，位于共享 Gradle 根工程的
`:jm-mobile-clock` 项目中。手表端源码保持在本目录，不与手机端
`app/` 混合。

## 导入

在 Android Studio 中打开仓库根目录：

```text
.
├─ app/
├─ jm-mobile-clock/
├─ gradle/
└─ settings.gradle.kts
```

不要单独打开本目录，也不要为本模块新增第二套 Gradle Wrapper、
版本目录或插件配置。

## 手表端检查

从仓库根目录执行：

```powershell
.\gradlew.bat :jm-mobile-clock:compileDebugKotlin --offline --no-daemon --max-workers=2 --console=plain
.\gradlew.bat :jm-mobile-clock:testDebugUnitTest --offline --no-daemon --max-workers=2 --console=plain
```

完整构建和签名发布请在本地或 CI 环境完成。签名文件、密钥和本地
配置必须保存在仓库外。

## 主要能力

- 圆形与方形表盘适配；
- 手表首页、菜单、详情、设置和收藏；
- 阅读控制、缩放、分页、重力翻页；
- 应用锁、图形锁、悬浮球和后台防窥；
- 离线缓存、下载进度、暂停/恢复、失败重试和本地阅读；
- AI 对话与联网搜索。
