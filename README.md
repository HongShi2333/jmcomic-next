# JMcomic Next

<img src=".\app\src\main\res\mipmap-mdpi\logo.webp" alt="logo" style="zoom:200%;" />

JMcomic Next 是一个基于 Kotlin、Jetpack Compose 与 Material 3 构建的 Android 漫画客户端。项目由 `Dedicatus546/jm-mobile` 二次开源整理而来，当前仓库在原项目基础上继续维护移动端界面、阅读体验、本地缓存、下载管理、PDF 导出与 AI 对话等功能。

感谢[INUXL DO社区](https://linux.do)中为我出主意的佬友们。以及对本项目提供大力支持的[RawChat团队](https://linux.do/u/RawChat)，没有各位的支持，这个项目不可能出现，由衷感谢各位！

项目的主要目标是把传统 Android 漫画客户端改造成更贴近现代 Android 设计习惯的 Compose 应用：页面使用 Material You 风格，入口和阅读流程尽量简洁，网络请求、登录状态、本地设置与缓存数据通过较清晰的 Repository、Manager、Storage、ViewModel 分层组织。

## 截图

| 首页                            | 详情（日间）                                            |
| ------------------------------- | ------------------------------------------------------- |
| ![首页](readme-assets/首页.jpg) | ![详情（日间模式）](readme-assets/详情（日间模式）.jpg) |

| 详情（夜间）                                            | 搜索                             |
| ------------------------------------------------------- | -------------------------------- |
| ![详情（夜间模式）](readme-assets/详情（夜间模式）.jpg) | ![搜索](readme-assets/搜索1.jpg) |

| 搜索结果                             | 每周必看                                |
| ------------------------------------ | --------------------------------------- |
| ![搜索结果](readme-assets/搜索2.jpg) | ![每周必看](readme-assets/每周必看.jpg) |

| 签到                            | 个人中心                                |
| ------------------------------- | --------------------------------------- |
| ![签到](readme-assets/签到.jpg) | ![个人中心](readme-assets/个人中心.jpg) |

## 功能概览

- 首页推荐：展示轮播推荐内容，并提供常用页面入口。
- 漫画搜索：支持关键词搜索、搜索结果列表、历史搜索记录与排序筛选。
- 漫画详情：展示封面、标题、作者、标签、章节、评论、相关推荐与收藏状态。
- 漫画阅读：支持滚动阅读和分页阅读，阅读页会按当前页前后范围预解码图片。
- 图片解码：针对部分图片切片/扰乱规则，客户端会根据漫画 ID、页码和服务端参数还原图片。
- 用户系统：支持登录、自动登录、签到、收藏列表、阅读历史与评论历史。
- 评论功能：支持查看漫画评论、发表评论和回复评论。
- 每周推荐：支持按分类和类型查看每周必看内容。
- 本地下载：使用 WorkManager 创建后台下载任务，下载封面和图片后写入本地缓存。
- 下载管理：使用 Room 保存下载任务、进度、状态、封面路径和压缩包路径。
- 本地阅读：已下载漫画可从本地缓存或 zip 包中读取图片。
- PDF 导出：已缓存漫画可通过系统目录授权导出为 PDF。
- AI 对话：提供本地会话管理和流式响应展示，支持可选的思考块展示逻辑。
- 本地设置：支持 API 域名、图片分流、主题模式、阅读模式和预加载数量等配置。
- 深色模式：通过 Material 3 主题适配浅色、深色和跟随系统模式。

## 技术栈

| 类型       | 使用内容                                             |
| ---------- | ---------------------------------------------------- |
| 语言       | Kotlin                                               |
| UI         | Jetpack Compose、Material 3、Material Icons Extended |
| 导航       | AndroidX Navigation Compose                          |
| 架构与状态 | ViewModel、StateFlow、Compose State                  |
| 依赖注入   | Koin                                                 |
| 网络       | Retrofit、OkHttp、Gson                               |
| 图片加载   | Coil                                                 |
| 本地数据库 | Room、Room Paging                                    |
| 分页       | Paging 3                                             |
| 后台任务   | WorkManager                                          |
| 日历组件   | Kizitonwose Calendar Compose                         |
| 构建       | Gradle Kotlin DSL、KSP、Android Gradle Plugin        |

## 环境要求

- Android Studio Narwhal 或更新版本。
- JDK 17 或更高版本。项目源码编译目标为 JVM 17。
- Android SDK：
  - `compileSdk 36`
  - `targetSdk 35`
  - `minSdk 23`
- Gradle Wrapper 已包含在仓库中，建议直接使用项目内的 `gradlew`。

项目当前使用阿里云 Maven 镜像源，配置位于 `settings.gradle.kts`。如果你处在其他网络环境，也可以把仓库源改回 `google()`、`mavenCentral()` 和 `gradlePluginPortal()`。

## 快速开始

克隆仓库：

```bash
git clone https://github.com/HongShi2333/jmcomic-next.git
cd jmcomic-next
```

使用 Android Studio 打开项目根目录，等待 Gradle 同步完成后运行 `app` 模块。

命令行检查 Kotlin 编译：

```bash
./gradlew :app:compileDebugKotlin --console=plain
```

Windows 环境：

```bat
gradlew.bat :app:compileDebugKotlin --console=plain
```

构建 Debug APK：

```bash
./gradlew :app:assembleDebug --console=plain
```

构建 Release APK：

```bash
./gradlew :app:assembleRelease --console=plain
```

首次打开项目时，Android Studio 会自动生成 `local.properties`。该文件包含本机 Android SDK 路径，不应提交到仓库。

## 应用信息

| 项                 | 当前值               |
| ------------------ | -------------------- |
| 应用名             | JMcomic              |
| Gradle rootProject | `jm-mobile-android6` |
| Android 模块       | `app`                |
| namespace          | `com.par9uet.jm`     |
| applicationId      | `jmcomicoi.net`      |
| minSdk             | `23`                 |
| targetSdk          | `35`                 |
| compileSdk         | `36`                 |
| 当前 versionName   | `1.1.2`              |
| License            | GPL-3.0              |

`applicationId` 是安装到设备后的包名，`namespace` 是源码和资源生成使用的命名空间。当前 Kotlin 源码和资源引用依赖 `com.par9uet.jm`，不建议随意修改 `namespace`。

## 项目结构

```text
.
├── app/                              Android 应用模块
│   ├── build.gradle.kts              app 模块构建配置
│   ├── proguard-rules.pro            Release 混淆配置
│   └── src/main/
│       ├── AndroidManifest.xml       应用清单、权限、入口 Activity
│       ├── java/com/par9uet/jm/      Kotlin 源码
│       └── res/                      图标、主题、字符串等资源
├── gradle/
│   ├── libs.versions.toml            依赖与插件版本管理
│   └── wrapper/                      Gradle Wrapper
├── readme-assets/                    README 截图资源
├── CHANGELOG                         变更记录
├── LICENSE                           GPL-3.0 许可证
├── README.md                         项目说明
├── settings.gradle.kts               仓库源和模块声明
└── version.properties                应用版本号配置
```

核心源码位于 `app/src/main/java/com/par9uet/jm`：

```text
com.par9uet.jm
├── cache/             缓存目录工具
├── coil/              Coil ImageLoader 配置
├── data/models/       业务数据模型
├── database/          Room 数据库、DAO、下载任务实体
├── di/                Koin 依赖注入模块
├── repository/        数据仓库，封装网络和业务请求
├── retrofit/          Retrofit、OkHttp、拦截器、响应转换和接口定义
├── storage/           SharedPreferences、安全存储和本地状态持久化
├── store/             全局状态管理器，如用户、设置、下载、Toast
├── task/              应用启动初始化任务接口
├── ui/                Compose 页面、组件、主题、ViewModel
├── utils/             通用工具、PDF 导出、图片压缩、时间和日志
├── worker/            WorkManager 后台任务
├── App.kt             Compose 应用根节点
├── JmApplication.kt   Application，启动 Koin 和 WorkManager 注入
└── MainActivity.kt    Activity 入口，启用 edge-to-edge 并挂载 Compose
```

## 启动流程

应用入口是 `MainActivity`。它会启用沉浸式边到边显示，并在 `setContent` 中挂载 `AppTheme` 和 `App`。

`JmApplication` 在应用启动时调用 `startKoin`，注册以下模块：

- `appModule`：协程作用域、Storage、Manager、AI 仓库、全局 ViewModel。
- `coilModule`：全局 `ImageLoader`。
- `comicModule`：漫画仓库和漫画相关 ViewModel。
- `retrofitModule`：Retrofit、OkHttp 拦截器和接口 Service。
- `userModule`：用户仓库和用户 ViewModel。
- `databaseModule`：Room 数据库、下载 DAO、下载 Manager、下载 Worker。

`App` 首次组合时会调用 `GlobalViewModel.init()`。初始化任务来自所有实现 `AppInitTask` 的对象，按 `AppTaskInfo.sort` 排序执行：

| 顺序 | 任务             | 作用                                         |
| ---- | ---------------- | -------------------------------------------- |
| 1    | Retrofit 配置    | 恢复 Cookie，准备网络层                      |
| 2    | 远端应用设置     | 请求远端设置，例如图片域名                   |
| 3    | 本地 APP 设置    | 读取本地主题、阅读模式、API 域名、分流等配置 |
| 其他 | 用户、历史搜索等 | 恢复用户状态和本地状态                       |

初始化完成后，`InitManager` 的 `deferred` 会被完成，网络请求可以通过 `InitInterceptor` 等机制等待初始化状态。

## 页面导航

主导航定义在 `ui/screens/AppScreen.kt`，使用 `NavHost` 管理页面。默认入口是 `tab/home`。

| 路由                                | 页面                        | 说明                    |
| ----------------------------------- | --------------------------- | ----------------------- |
| `tab/{tabName}`                     | `TabScreen`                 | 底部 Tab 容器，默认首页 |
| `login`                             | `LoginScreen`               | 登录页                  |
| `comicDetail/{id}`                  | `ComicDetailScreen`         | 漫画详情                |
| `comicRead/{id}`                    | `ComicReadScreen`           | 在线阅读                |
| `localComicRead/{id}`               | `ComicReadScreen`           | 本地缓存阅读            |
| `comicSearch`                       | `ComicSearchScreen`         | 搜索入口                |
| `comicSearchResult/{searchContent}` | `ComicSearchResultScreen`   | 搜索结果                |
| `comicRecommend`                    | `ComicWeekRecommendScreen`  | 每周推荐                |
| `comment/{comicId}`                 | `ComicCommentScreen`        | 评论列表                |
| `sign`                              | `SignInScreen`              | 签到页                  |
| `download`                          | `DownloadScreen`            | 下载管理                |
| `downloadComicDetail/{id}`          | `DownloadComicDetailScreen` | 已缓存漫画详情          |
| `userCollectComic`                  | `UserCollectComicScreen`    | 用户收藏                |
| `userHistoryComic`                  | `UserHistoryComicScreen`    | 阅读历史                |
| `userHistoryComment`                | `UserHistoryCommentScreen`  | 评论历史                |
| `appLocalSetting`                   | `LocalSettingScreen`        | 本地设置                |

`TabScreen` 负责底部导航框架，其他详情页、阅读页、下载页等通过主导航进入。

## 网络层

网络层由 `retrofit/Retrofit.kt` 封装。项目使用一个占位 baseUrl 创建 Retrofit，真正的 API 地址通过 `BaseUrlInterceptor` 动态替换。

主要组成：

- `ComicService`：漫画详情、搜索、推荐、章节图片、评论、收藏和点赞。
- `UserService`：登录、收藏列表、阅读历史、评论历史、签到信息和签到提交。
- `RemoteSettingService`：远端设置。
- `BaseUrlInterceptor`：根据本地设置切换 API 域名。
- `TokenInterceptor`：附加接口所需的鉴权或签名参数。
- `InitInterceptor`：处理初始化完成前的请求时序。
- `ToastInterceptor`：把网络错误转成用户可见提示。
- `ResponseConverterFactory`：解析接口响应，并在成功时对加密的 `data` 字段解密。
- `PrimitiveToRequestBodyConverterFactory`：支持 Retrofit `@Part` 传递基础类型。

漫画图片列表不是标准 JSON 接口。`ComicRepositoryImpl.getComicPicList()` 会请求 `chapter_view_template`，拿到 HTML 后由 `DataDecode.kt` 中的 `parseHtml()`、`parseRange()` 和 `parseSpeed()` 解析图片地址、扰乱阈值和速度参数。

## 数据与状态

项目整体采用“Service -> Repository -> ViewModel -> Compose UI”的数据流。

```text
Retrofit Service / Room DAO / Storage
        ↓
Repository
        ↓
ViewModel
        ↓
StateFlow / Compose State
        ↓
Composable Screen
```

常见 UI 状态封装在 `ui/models` 中，例如 `CommonUIState`、`ListUIState`、`PageAppendUIState`。分页列表使用 Paging 3，分页源位于 `ui/pagingSource`。

全局状态类主要放在 `store`：

| 类                     | 作用                                    |
| ---------------------- | --------------------------------------- |
| `UserManager`          | 管理用户登录态和用户信息                |
| `RemoteSettingManager` | 管理远端配置                            |
| `LocalSettingManager`  | 管理主题、API、分流、阅读模式等本地设置 |
| `HistorySearchManager` | 管理搜索历史                            |
| `DownloadManager`      | 创建下载任务                            |
| `ToastManager`         | 通过 Flow 向全局 Snackbar 发送提示      |
| `InitManager`          | 协调应用初始化完成状态                  |

## 本地存储

项目使用两类本地存储：

- `SharedPreferences`：保存用户信息、Cookie、本地设置、搜索历史和 AI 会话。
- `Room`：保存下载漫画任务和缓存信息。

`SecureStorage` 对写入 SharedPreferences 的内容做统一序列化和加密处理。`CryptoManager` 优先使用 Android Keystore 的 AES/GCM 密钥；如果 Keystore 不可用，会退化为带 `plain:` 前缀的 Base64 明文编码，便于兼容异常设备。

Room 数据库定义在 `database/AppDatabase.kt`，当前只包含 `DownloadComic` 一张表：

| 字段         | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| `id`         | 漫画 ID，作为主键                                          |
| `name`       | 漫画名称                                                   |
| `authorList` | 作者列表                                                   |
| `coverPath`  | 本地封面路径                                               |
| `zipPath`    | 本地图片压缩包路径                                         |
| `progress`   | 下载进度，范围通常是 `0f..1f`                              |
| `status`     | 下载状态，如 `pending`、`downloading`、`complete`、`error` |
| `createTime` | 创建时间戳                                                 |

## 下载与缓存

下载入口位于 `DownloadManager.downloadComic()`。它会先向 Room 插入一条状态为 `pending` 的下载记录，然后创建 `OneTimeWorkRequest` 交给 WorkManager 执行。

`DownloadComicWorker` 的处理流程：

1. 读取输入参数 `comicId`。
2. 把任务状态更新为 `downloading`。
3. 根据远端图片域名下载封面。
4. 请求章节图片 HTML，并解析得到图片地址列表。
5. 使用 `ComicPicImageState.decode()` 下载并解码每一页图片。
6. 将解码后的图片以 WebP 写入缓存目录。
7. 更新 Room 中的下载进度。
8. 把图片目录压缩为 zip。
9. 保存 zip 路径，并把任务状态更新为 `complete`。
10. 如果失败，最多重试 3 次，最终把任务状态更新为 `error`。

缓存目录由 `cache/Config.kt` 定义：

| 方法                           | 目录                            |
| ------------------------------ | ------------------------------- |
| `getCommonCacheDir()`          | `cacheDir/common`               |
| `getCommonPicDecodeCacheDir()` | `cacheDir/pic_decode/{comicId}` |
| `getDownloadDir()`             | `cacheDir/download`             |

下载完成后，本地阅读会优先读取 `cacheDir/download/{comicId}`。如果图片目录不存在，会尝试从 zip 包解压出图片。

## 图片解码

`ComicPicImageState` 负责图片加载、扰乱还原和本地解码缓存。

处理逻辑包括：

- 使用 Coil 以原始尺寸加载图片，避免缩放导致解码后出现白线。
- 如果图片是 GIF、漫画 ID 小于等于扰乱阈值，或服务端速度参数为 `1`，则不做切片还原。
- 对需要还原的图片，根据 `comicId + page` 的 MD5 计算 seed。
- 按 seed 将原图纵向分片，并重新绘制为正确顺序。
- 解码完成后缓存为 WebP，下次阅读可直接读取缓存文件。

阅读页通过 `ComicReadViewModel.decodeIndex()` 处理预加载。默认预加载数量来自 `LocalSetting.prefetchCount`，当前默认值是 `3`。

## PDF 导出

PDF 导出工具位于 `utils/PdfExport.kt`。导出流程依赖 Android 系统的目录授权 URI，而不是直接写入外部存储路径。

核心行为：

- 读取已缓存漫画的图片目录。
- 如果目录不存在但 zip 存在，会先解压 zip。
- 按页码文件名排序图片。
- 每张图片创建一页同尺寸 PDF 页面。
- 通过 `DocumentsContract.createDocument()` 在用户授权目录创建 PDF。

导出的文件名格式为：

```text
{漫画名}_{漫画ID}.pdf
```

文件名中的非法字符会被替换为 `_`。

## AI 对话

AI 对话相关代码位于 `AiChatRepository`、`AiChatStorage`、`AiChatViewModel` 和 `AiChatScreen`。

当前实现特点：

- 会话保存在 `SecureStorage` 中。
- ViewModel 支持创建、选择、删除会话。
- 发送消息后会追加用户消息和空的助手消息，再流式填充助手回复。
- 当启用“思考”模式时，会在请求消息前插入一条 system 消息，要求上游返回 `<think>...</think>` 思考块。
- Repository 使用 OkHttp 直接请求上游服务，并逐行读取流式响应。

需要注意的是，`AiChatRepository` 中的 `DEVICE_ID` 和 `COOKIES` 当前为空。若上游服务要求有效设备标识或 Cookie，AI 请求可能返回 HTML、鉴权错误或限制提示。

## 本地设置

`LocalSetting` 定义了应用的本地可配置项：

| 配置                     | 默认值             | 说明                         |
| ------------------------ | ------------------ | ---------------------------- |
| `api`                    | `apiList[0]`       | 当前 API 域名                |
| `apiList`                | 多个内置域名       | 可用于切换 API 入口          |
| `theme`                  | `auto`             | 支持 `auto`、`light`、`dark` |
| `shunt`                  | `1`                | 图片分流                     |
| `shuntList`              | `1`、`2`、`3`、`4` | 可选图片分流                 |
| `prefetchCount`          | `3`                | 阅读页前后预解码图片数量     |
| `readMode`               | `scroll`           | 支持滚动阅读和分页阅读       |
| `showComicScrollReadTip` | `true`             | 是否展示滚动阅读提示         |
| `showComicPageReadTip`   | `true`             | 是否展示分页阅读提示         |

`LocalSettingManager` 会在应用初始化时从本地读取设置，并在用户修改后立即写回本地存储。

## 发布与版本

版本信息位于 `version.properties`：

```properties
VERSION_CODE=1.1.2
VERSION_NAME=1.1.2
```

`app/build.gradle.kts` 会读取该文件，并把 `VERSION_NAME` 作为 `versionName`。Android 的 `versionCode` 通常应为整数，如果构建工具对当前 `VERSION_CODE=1.1.2` 报错，建议改为类似下面的格式：

```properties
VERSION_CODE=112
VERSION_NAME=1.1.2
```

Release 构建开启了混淆和资源压缩：

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

APK 输出文件名会带上版本号和 Git 短 hash：

```text
jm-mobile_v{versionName}_{gitHash}.apk
```

仓库包含 GitHub Actions 发布流程 `.github/workflows/release.yml`，在推送 `v*.*.*` 标签时构建 Release APK 并创建草稿 Release。当前工作流里解码签名文件的文件名和 Gradle 参数中的文件名不一致：前者写入 `release-key.jks`，后者使用 `release.jks`。如果要启用自动发布，需要先统一这两个文件名，并配置对应的签名密钥 Secrets。

## 开发说明

新增页面时，通常需要：

1. 在 `ui/screens` 下创建新的 Compose 页面。
2. 如果页面需要业务状态，在 `ui/viewModel` 中添加 ViewModel。
3. 在对应的 Koin 模块中注册 ViewModel。
4. 在 `AppScreen.kt` 中添加导航路由。
5. 如果需要底部 Tab 入口，在 `tabScreen` 相关组件中补充入口。

新增接口时，通常需要：

1. 在 `retrofit/service` 中为对应 Service 添加 Retrofit 方法。
2. 在 `retrofit/model` 中添加响应模型。
3. 在 `repository` 中添加抽象方法。
4. 在 `repository/impl` 中通过 `safeApiCall` 或 `safeStringCall` 包装请求。
5. 在 ViewModel 中调用 Repository，并转成 UI 状态。

新增本地持久化数据时，可根据数据性质选择：

- 简单配置或小体量状态：放入 `storage`，通过 `SecureStorage` 保存。
- 需要查询、分页、状态更新的数据：新增 Room Entity、DAO 和 Database 配置。

新增下载类后台任务时，应优先使用 WorkManager，并在 `databaseModule` 中通过 Koin 注册 Worker，避免直接在 UI 层启动长时间任务。

## 常见问题

### Gradle 同步失败

优先检查 Android Studio、JDK、Android SDK 和 Gradle 插件版本。项目当前使用较新的 AGP、Kotlin 和 Compose BOM，如果本地 Android Studio 版本过旧，可能无法识别插件或 SDK。

### API 请求失败

先在设置页切换 API 域名，再确认设备网络可访问对应服务。项目的 baseUrl 是运行时动态替换的，实际请求地址取决于本地设置和拦截器。

### 图片加载成功但显示异常

阅读页依赖原始尺寸图片和本地解码缓存。若图片出现白线、错位或旧缓存问题，可以尝试清理应用缓存后重新打开漫画。

### 下载任务一直失败

下载依赖网络、远端图片域名、图片解码和本地缓存写入。可以先确认在线阅读是否正常，再检查 Room 中任务状态和缓存目录是否存在对应图片。

### PDF 导出失败

PDF 导出需要系统目录授权。请选择可写入的目录，并确认对应漫画已经完成缓存且本地图片可读取。

### AI 对话不可用

AI 对话依赖上游服务状态、Cookie 和设备标识。当前仓库中相关常量为空，如果上游服务限制未登录访问，请先补充自己的合法访问配置。（理论上来讲这家它是不做这些参数的硬性要求的，不加也能跑）

## 已知注意点

- `ComicService.collectComic()` 同时被 `collectComic()` 和 `unCollectComic()` 调用，取消收藏是否真正生效取决于上游接口行为。
- `DownloadComic.status` 注释只写了 `pending || downloading || complete`，DAO 和 Worker 实际还会使用 `error`、`paused` 等状态，后续可统一为枚举或 sealed class。
- `Coil` 磁盘缓存配置注释写的是 `200MB`，实际代码为 `1024L * 1024 * 1024`，即约 1GB。
- `version.properties` 中的 `VERSION_CODE` 当前不是整数，如果构建时报版本号错误，需要按 Android 规范改为整数。
- `.github/workflows/release.yml` 中签名文件名存在不一致，启用自动发布前需要修正。
- Release 构建目前仍使用 debug signingConfig，正式分发前应改为 release 签名配置。

## 二次开源说明

本项目基于 `Dedicatus546/jm-mobile` 继续整理和维护。当前仓库重点放在以下方向：

- 统一 Compose 页面风格，使主要页面更接近 Material You 设计语言。
- 修复阅读进度条、登录页、AI 对话解析、本地缓存详情等使用问题。
- 增加缓存漫画详情展示和 PDF 导出流程。
- 支持 Android 6.0 及以上设备运行。

## 免责声明

本项目仅供学习、研究和技术交流使用。项目作者与任何第三方服务、原始应用或内容提供方无关。

使用者应自行遵守当地法律法规以及相关服务条款。因使用本项目产生的任何法律、版权、账号、数据或财务风险均由使用者自行承担。

## License

本项目遵循仓库中的 `LICENSE` 文件，许可证为 GPL-3.0。原项目版权和许可证信息请参考 `Dedicatus546/jm-mobile`。
