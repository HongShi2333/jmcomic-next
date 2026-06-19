# JMcomic

JMcomic 是一个基于 Kotlin 与 Jetpack Compose 构建的 Android 漫画客户端。本项目由 [Dedicatus546/jm-mobile](https://github.com/Dedicatus546/jm-mobile) 二次开源整理而来，在原项目基础上继续维护移动端体验、阅读流程、本地缓存、AI 对话和 Material You 风格界面。

项目目标是提供一个更符合现代 Android 设计习惯的 Compose 客户端：界面遵循 Material You / Material 3 设计语言，交互尽量贴近系统组件，阅读页、详情页、个人中心、登录页等核心页面统一使用更简洁的视觉层级与深色/浅色主题适配。

## 截图

| 首页 | 详情（日间） |
| --- | --- |
| ![首页](readme-assets/首页.jpg) | ![详情（日间模式）](readme-assets/详情（日间模式）.jpg) |

| 详情（夜间） | 搜索 |
| --- | --- |
| ![详情（夜间模式）](readme-assets/详情（夜间模式）.jpg) | ![搜索](readme-assets/搜索1.jpg) |

| 搜索结果 | 每周必看 |
| --- | --- |
| ![搜索结果](readme-assets/搜索2.jpg) | ![每周必看](readme-assets/每周必看.jpg) |

| 签到 | 个人中心 |
| --- | --- |
| ![签到](readme-assets/签到.jpg) | ![个人中心](readme-assets/个人中心.jpg) |

## 功能特性

- 首页内容浏览：支持推荐内容、分类入口和常用页面跳转。
- 漫画搜索：支持关键词搜索、结果列表、历史记录和筛选浏览。
- 详情页：展示封面、标题、作者、标签、章节、评论和相关推荐。
- 阅读页：支持漫画图片阅读、翻页/滚动阅读、阅读进度显示与跳转。
- 用户系统：支持登录、签到、收藏、历史记录和个人数据展示。
- 本地缓存：支持下载漫画到本地，并在缓存完成后查看详细信息。
- PDF 导出：已缓存漫画可以导出为 PDF，目录由系统文件选择器授权。
- AI 对话：提供对话页面，并对深度思考内容与正文展示做分离处理。
- Material You：基于 Material 3 组件体系，适配浅色/深色主题与动态色风格。

## 二次开源说明

本项目基于原开源项目 [Dedicatus546/jm-mobile](https://github.com/Dedicatus546/jm-mobile) 继续整理和维护。当前仓库重点放在以下方向：

- 统一 Compose 页面风格，使主要页面更接近 Material You 设计语言。
- 修复阅读进度条、登录页、AI 对话解析、本地缓存详情等使用问题。
- 增加缓存漫画详情展示和 PDF 导出流程。
- 支持 Android 6.0 及以上设备运行。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation
- Koin
- Retrofit / OkHttp
- Coil
- Room
- WorkManager
- Kotlin Symbol Processing

## 环境要求

- Android Studio Narwhal 或更新版本
- JDK 17
- Android SDK
  - `compileSdk 36`
  - `minSdk 23`
  - `targetSdk 35`

## 构建与运行

克隆仓库后，用 Android Studio 打开项目根目录，等待 Gradle 同步完成后运行 `app` 模块。

如果需要在命令行检查编译：

```bash
./gradlew :app:compileDebugKotlin --console=plain
```

Windows 环境可使用：

```bat
gradlew.bat :app:compileDebugKotlin --console=plain
```

首次打开项目时，Android Studio 会在本地生成 `local.properties`。该文件包含本机 SDK 路径，不应提交到仓库。

## 项目结构

```text
app/                         Android 应用模块
app/src/main/java/           Kotlin 与 Compose 源码
app/src/main/res/            图标、主题、字符串等资源
gradle/                      Gradle Wrapper 配置
readme-assets/               README 截图资源
version.properties           应用版本号配置
```

## 当前应用信息

```text
应用名：JMcomic
applicationId：jmcomicoi.net
namespace：com.par9uet.jm
versionName：1.1.1
```

说明：`applicationId` 是安装到手机后的包名；`namespace` 是源码和资源生成使用的命名空间。当前源码中的 `R` 资源引用依赖 `com.par9uet.jm`，因此不建议随意修改 `namespace`。

## 发布前检查

提交或发布前建议确认：

- `:app:compileDebugKotlin` 可以通过
- 不包含 `local.properties`
- 不包含 `.gradle/`、`.idea/`、`.kotlin/`
- 不包含 `app/build/`、`app/release/`
- 不包含 APK、AAB、签名密钥、Cookie、Token 或其他私有配置
- 截图、README、版本号和 License 信息已经更新

## 免责声明

本项目仅供学习、研究和技术交流使用。项目作者与任何第三方服务、原始应用或内容提供方无关。使用者应自行遵守当地法律法规以及相关服务条款。因使用本项目产生的任何法律、版权、账号、数据或财务风险均由使用者自行承担。

## License

本项目遵循仓库中的 `LICENSE` 文件。原项目版权和许可证信息请参考 [Dedicatus546/jm-mobile](https://github.com/Dedicatus546/jm-mobile)。

