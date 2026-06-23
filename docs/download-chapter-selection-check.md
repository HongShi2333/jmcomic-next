# 详情页章节下载改动留档

## 改动范围

- 详情页下载按钮：多章节漫画不再直接下载当前详情 id，而是打开章节选择弹窗。
- 章节选择弹窗：支持勾选单章/多章，并提供全选、清空、前20话、后20话快捷选择。
- 下载管理器：新增批量下载入口，批量选择时每个章节独立创建 WorkManager 任务，互不绑定；同时写入父漫画/章节元数据，并放宽下载重试次数、重试间隔和单页超时，提升慢图/偶发网络抖动下的成功率。
- 下载表结构：升级到 Room v3，新增 `parentId`、`parentName`、`chapterIndex`、`chapterName`、`chapterCount`，并通过 2->3 migration 兼容旧记录。
- 下载中心：通过单一全量下载记录 Flow 按 `parentId` 聚合成漫画卡片，再按整组状态放入完成、正在缓存、错误分区；卡片摘要显示本数/话数、聚合进度和状态计数，展开后显示具体章节。
- 阅读入口：详情页“第1话”优先跳转到 `comicChapterList.first().id`，避免主作品 id 与章节 id 混用。

## 功能检查

- 单章节或无章节漫画：点击详情页下载按钮后，应直接创建 1 个下载任务。
- 多章节漫画：点击详情页下载按钮后，应展示“选择下载章节”弹窗。
- 弹窗默认选择全部章节，用户可以取消任意章节。
- 章节很多时，可以用“全选”“清空”“前20话”“后20话”快速调整选择范围。
- 未选择章节时，“下载”按钮不可用。
- 确认下载后，每个被选章节都会以独立章节 id 创建下载任务。
- 确认下载后，下载记录会保留父漫画 id/name 与章节序号/name；下载中心按这些字段聚合展示，同一本漫画不会因为多话下载占满列表。

## 注意事项

- 下载任务当前最多尝试 6 次，WorkManager 线性 backoff 为 30 秒；单页图片下载/解码超时为 180 秒。
- 章节重试时会复用已经写入 `cacheDir/download/{chapterId}` 的图片文件，并且只在新进度大于当前进度时更新 Room，避免 90% 回退到 1% 的视觉倒退。
- 多章节批量下载时，每个章节对应独立 Worker；如果第三、第四话等章节失败，优先检查对应章节的单页下载/解码日志和远端图片解析结果。
- 当前本地下载表仍以章节 id 作为主键；重复下载同一章节会按现有 `REPLACE` 策略刷新任务。
- 旧下载记录在 2->3 migration 中回填为自父级记录：`parentId = id`、`parentName = name`，避免升级后列表读取失败。
- Worker 仍按单章节 id 下载图片、封面和 zip；本次没有改变底层缓存目录结构。
- 下载中心 UI 已按 `parentId`/`parentName` 做整本聚合；同一本漫画不会因为部分章节完成、部分章节仍在缓存而拆成多张卡，底层 Worker 和缓存目录仍保持章节级，方便单话失败、重试和阅读。

## PR 说明草稿

### Summary

- 详情页多章节漫画点击下载时，改为打开章节多选弹窗。
- 新增批量创建下载任务能力，每个选中章节仍复用原有单章节 WorkManager 下载链路。
- 下载表升级到 v3，并为章节任务保存父漫画 id/name、章节序号/name 和总章节数。
- 下载中心从章节列表改为漫画分组卡片，组内可展开查看章节；同组按活动任务、错误、完成的优先级归入单个分区，进行中/错误分区可对整组任务执行取消/删除。
- 修正详情页“第1话”入口，优先跳转到第一条章节 id。
- 增加本留档，记录交互检查点和后续整本聚合展示的扩展方向。

### Checks

- `git diff --check -- app/src/main/java/com/par9uet/jm/database/dao/DownloadComicDao.kt app/src/main/java/com/par9uet/jm/database/model/DownloadComic.kt app/src/main/java/com/par9uet/jm/database/AppDatabase.kt app/src/main/java/com/par9uet/jm/di/DatabaseModule.kt app/src/main/java/com/par9uet/jm/store/DownloadManager.kt app/src/main/java/com/par9uet/jm/ui/screens/ComicDetailScreen.kt app/src/main/java/com/par9uet/jm/ui/screens/readScreen/ComicReadScreen.kt app/src/main/java/com/par9uet/jm/worker/DownloadComicWorker.kt app/src/main/java/com/par9uet/jm/ui/viewModel/DownloadViewModel.kt app/src/main/java/com/par9uet/jm/ui/screens/downloadScreen/DownloadScreen.kt app/src/main/java/com/par9uet/jm/ui/screens/downloadScreen/DownloadListItem.kt app/src/main/java/com/par9uet/jm/ui/screens/downloadScreen/DownloadComicDetailScreen.kt docs/download-chapter-selection-check.md`：通过，仅有 Git 对 LF/CRLF 的提示。
- `gradle :app:compileDebugKotlin --offline --no-daemon --stacktrace`：已通过本机 Gradle 9.5.1 分发包启动，但当前项目 `gradle/gradle-daemon-jvm.properties` 要求 daemon JDK 21；本机仅找到 JDK 17，Gradle 尝试从 `api.foojay.io` 下载 JDK 21 toolchain 时被沙箱网络拦截（`Permission denied: getsockopt`）。需要安装/配置本地 JDK 21，或在允许网络的环境补跑。
