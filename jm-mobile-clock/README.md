# jm-mobile-clock

`jm-mobile-clock` 是 `jm-mobile-android6` 的手表端应用模块。它与手机/平板端共用同一个 Gradle 根工程、wrapper、版本目录和依赖缓存，目录内不会再维护一套项目级 Gradle 配置。

## 手表端范围

- 保留：发现、搜索、详情、章节选择、在线阅读、登录、线上收藏、应用锁、阅读进度、剪切板编码检测、主题与界面缩放。
- 移除：封面、漫画页和下载内容的本地持久化缓存。图片仅在当前页面内存中解码，离开即释放。
- 适配：圆形/方形小屏安全边距、单列信息架构、旋转表冠滚动、PIN 键盘、大触控区域、OLED 深色主题和 85%/100%/115% 视觉缩放。

## 打开方式

请始终从 `jm-mobile-android6` 目录打开 Android Studio 或运行 Gradle。模块名为 `:jm-mobile-clock`，本目录没有也不需要 `settings.gradle.kts`、Gradle wrapper、`gradle.properties` 或 `.gradle` 缓存。

本地最终验证可在 `jm-mobile-android6` 根目录执行 `gradlew.bat :jm-mobile-clock:assembleDebug`。
