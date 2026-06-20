# JM Mobile Android

一个基于 Kotlin、Jetpack Compose 和 Material You 的 Android 漫画客户端。

本仓库整理为可直接发布到 GitHub 的源码项目，不包含本机缓存、构建产物、签名密钥、账号 Cookie 或私有配置。

## 功能概览

- 首页、搜索、每周推荐、详情、评论、收藏、历史记录等漫画浏览功能
- 账号登录、签到、个人中心与用户数据展示
- 阅读页支持翻页/滚动阅读、阅读进度跳转和阅读状态保存
- 本地缓存漫画，缓存完成后可查看详情并导出 PDF
- AI 对话页面，支持深度思考内容与正文分离展示
- Material You 风格界面，支持浅色/深色主题

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
- Android Gradle Plugin 与 Gradle Wrapper 使用仓库内配置
- Android SDK：
  - `compileSdk 36`
  - `minSdk 23`
  - `targetSdk 35`

## 本地运行

1. 克隆仓库。
2. 用 Android Studio 打开项目根目录。
3. 确认本机存在 `local.properties`，并指向 Android SDK。
4. 同步 Gradle。
5. 运行 `app` 模块。

也可以在命令行检查编译：

```bat
gradlew.bat :app:compileDebugKotlin --console=plain
```

## 版本与应用信息

版本号位于根目录：

```properties
VERSION_CODE=1
VERSION_NAME=1.1.1
```

安装包名位于：

```kotlin
// app/build.gradle.kts
namespace = "com.par9uet.jm"
applicationId = "jmcomicoi.net"
```

说明：

- `applicationId` 是 Android 系统识别安装包的包名，改它即可改变安装包名。
- `namespace` 是源码和资源生成使用的命名空间，当前应保持 `com.par9uet.jm`，不要随意修改，否则 `R` 资源引用会失效。

应用名位于：

```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="app_name">JMcomic</string>
```

图标配置位于：

```xml
<!-- app/src/main/AndroidManifest.xml -->
android:icon="@mipmap/logo"
android:roundIcon="@mipmap/logo_round"
```

根目录提供了快速修改脚本：

```bat
set_app_info.bat
```

`set_app_info.bat` 会调用同目录的 `set_app_info.ps1`，用于修改应用名、`applicationId`、版本号和图标资源引用。直接回车会跳过对应项。发布或移动项目时，这两个脚本需要一起保留。

## GitHub 发布包

发布到 GitHub 时建议只提交 `github-code` 目录中的内容。

发布包应包含：

```text
.github/
app/
gradle/
readme-assets/
.gitignore
build.gradle.kts
CHANGELOG
gradle.properties
gradlew
gradlew.bat
LICENSE
README.md
settings.gradle.kts
set_app_info.bat
set_app_info.ps1
version.properties
修改.txt
```

发布包不应包含：

```text
.git/
.gradle/
.gradle-home/
.idea/
.kotlin/
build/
app/build/
app/release/
local.properties
*.apk
*.aab
签名密钥
账号、Cookie、Token 或其他私有配置
```

## 目录结构

```text
app/                         Android 应用模块
app/src/main/java/           Kotlin 源码
app/src/main/res/            Android 资源
gradle/                      Gradle Wrapper 配置
readme-assets/               README 截图资源
version.properties           版本号配置
set_app_info.bat             应用信息修改入口脚本
set_app_info.ps1             应用信息修改逻辑脚本
修改.txt                     应用信息修改说明
```

## 截图

截图资源位于 `readme-assets/`：

```text
readme-assets/首页.jpg
readme-assets/个人中心.jpg
readme-assets/签到.jpg
readme-assets/详情（日间模式）.jpg
readme-assets/详情（夜间模式）.jpg
readme-assets/搜索1.jpg
readme-assets/搜索2.jpg
readme-assets/每周必看.jpg
```

## 发布前检查

- `:app:compileDebugKotlin` 可以通过
- 没有提交 `local.properties`
- 没有提交 `.gradle/`、`.gradle-home/`、`.idea/`、`.kotlin/`
- 没有提交 `app/build/`、`app/release/`、APK 或 AAB
- 没有提交签名密钥、Cookie、Token 或私有接口配置
- README、LICENSE、版本号和截图已更新

## 免责声明

本项目仅供学习、研究和技术交流使用。项目作者与任何第三方服务、原始应用或内容提供方无关。使用者应自行遵守当地法律法规以及相关服务条款。因使用本项目产生的任何法律、版权、账号、数据或财务风险均由使用者自行承担。

## License

本项目遵循仓库中的 `LICENSE` 文件。
