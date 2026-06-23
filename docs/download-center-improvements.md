# 下载中心改进文档（最终版本）

## 修改日期
2026年6月22日

## 问题描述

### 问题1：下载中心章节折叠限制
- **现象**：当一个漫画下载超过5话时，在下载中心展开后只显示前6话，其余章节显示"还有X话，已收起以节省空间"，但无法点击查看
- **影响**：用户无法从下载中心直接访问超过第6话的章节

### 问题2：本地阅读时无法切换章节
- **现象**：在阅读已下载的漫画时，点击画面中央呼出工具栏，但工具栏中的章节切换功能不可用
- **根本原因**：本地阅读模式下调用了 `clearComicDetail()`，清空了章节列表数据，导致无法获取同一漫画的其他章节信息
- **影响**：用户在阅读本地缓存时无法使用工具栏切换章节，必须退出阅读页面重新选择

## 解决方案

### 1. 移除章节折叠限制

#### 修改文件：`DownloadListItem.kt`

**修改内容：**
- 删除 `MAX_EXPANDED_CHAPTER_ROWS = 6` 常量限制
- 移除 `take()` 方法，显示所有章节
- 删除"还有X话，已收起"的提示文本

**修改前：**
```kotlin
private const val MAX_EXPANDED_CHAPTER_ROWS = 6

// ...
val visibleChapters = group.sortedChapters.take(MAX_EXPANDED_CHAPTER_ROWS)
visibleChapters.forEach { chapter ->
    DownloadChapterRow(...)
}
val hiddenCount = group.chapterSize - visibleChapters.size
if (hiddenCount > 0) {
    Text("还有 $hiddenCount 话，已收起以节省空间")
}
```

**修改后：**
```kotlin
// Removed MAX_EXPANDED_CHAPTER_ROWS limit to show all chapters

// ...
group.sortedChapters.forEach { chapter ->
    DownloadChapterRow(...)
}
```

### 2. 修复本地阅读章节导航

#### 问题分析

在 `ComicReadScreen.kt` 中，本地阅读模式的初始化逻辑：

```kotlin
if (localOnly) {
    comicReadViewModel.clearComicDetail()  // ❌ 清空了 comic 数据
    comicReadViewModel.getLocalComicPicList(comicId, context, onSuccess)
}
```

这导致：
1. `comic` 为 `null`
2. `comic?.comicChapterList` 无法获取
3. 工具栏中的"章节"按钮被禁用（`enabled = comic?.comicChapterList?.isNotEmpty() == true`）
4. 底部的"上一话"/"下一话"按钮被禁用（`previousChapter` 和 `nextChapter` 为 `null`）

#### 修改文件1：`DownloadComicDao.kt`

**新增方法：**
```kotlin
@Query("SELECT * FROM download_comics WHERE parentId = :parentId OR (id = :parentId AND parentId = 0) ORDER BY chapterIndex ASC, createTime ASC")
suspend fun getChaptersByParent(parentId: Int): List<DownloadComic>
```

**说明：**
- 根据父漫画ID查询所有章节
- 兼容旧数据（parentId = 0 的情况）
- 按章节索引和创建时间排序

#### 修改文件2：`ComicReadViewModel.kt`

**新增导入：**
```kotlin
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.database.model.DownloadComic
```

**新增方法：**
```kotlin
fun loadLocalComicChapters(comicId: Int) {
    viewModelScope.launch {
        val currentComic = downloadComicDao.getById(comicId)
        if (currentComic != null) {
            val parentId = if (currentComic.parentId != 0) {
                currentComic.parentId
            } else {
                currentComic.id
            }

            val allChapters = downloadComicDao.getChaptersByParent(parentId)
                .sortedWith(compareBy<DownloadComic> { it.chapterIndex }
                    .thenBy { it.createTime }
                    .thenBy { it.id })
                .filter { it.status == "complete" }  // 只显示已完成的章节

            if (allChapters.isNotEmpty()) {
                val chapterList = allChapters.map { chapter ->
                    ComicChapter(
                        id = chapter.id,
                        name = buildChapterName(chapter)
                    )
                }

                _comicDetailState.update {
                    it.copy(
                        data = Comic.create(
                            id = parentId,
                            name = currentComic.parentName.ifBlank { currentComic.name },
                            authorList = currentComic.authorList
                        ).copy(comicChapterList = chapterList)
                    )
                }
            }
        }
    }
}

private fun buildChapterName(chapter: DownloadComic): String {
    val hasChapterMetadata = chapter.parentId != chapter.id ||
        chapter.chapterCount > 1 ||
        chapter.chapterName.isNotBlank()

    if (!hasChapterMetadata) {
        return chapter.name
    }

    val numberText = "第" + (chapter.chapterIndex + 1) + "话"
    return if (chapter.chapterName.isBlank()) {
        numberText
    } else {
        numberText + " " + chapter.chapterName
    }
}
```

**关键点：**
1. 从数据库加载同组的所有已完成章节
2. 过滤 `status == "complete"`，确保只显示可阅读的章节
3. 使用 `Comic.create()` 创建轻量级的 `Comic` 对象
4. 构建符合格式的章节名称（"第X话" 或 "第X话 章节名"）

#### 修改文件3：`ComicReadScreen.kt`

**修改初始化逻辑：**

**修改前：**
```kotlin
if (localOnly) {
    comicReadViewModel.clearComicDetail()
    comicReadViewModel.getLocalComicPicList(comicId, context, onSuccess)
}
```

**修改后：**
```kotlin
if (localOnly) {
    comicReadViewModel.loadLocalComicChapters(comicId)  // ✅ 加载章节列表
    comicReadViewModel.getLocalComicPicList(comicId, context, onSuccess)
}
```

## 技术细节

### 章节分组逻辑
- 使用 `parentId` 字段识别同一漫画的不同章节
- 如果 `parentId == 0`，则将 `id` 作为分组依据（兼容旧数据）
- 按 `chapterIndex`、`createTime`、`id` 排序保证顺序正确

### 章节过滤
- 只显示 `status == "complete"` 的章节
- 避免显示下载中或出错的章节，防止用户点击无法阅读

### 数据流
```
本地阅读启动
    ↓
loadLocalComicChapters(comicId)
    ↓
查询数据库 → 获取同组章节 → 过滤已完成 → 构建章节列表
    ↓
更新 comicDetailState
    ↓
UI 层获取章节数据
    ↓
工具栏按钮启用
    ↓
用户可以切换章节
```

### UI 交互流程

#### 阅读页工具栏
1. 点击画面中央 → 显示工具栏
2. 左侧"章节"按钮（现在可用）
3. 点击 → 弹出章节选择对话框
4. 选择章节 → 导航到新章节 → 继续阅读

#### 底部导航栏
1. 显示当前页码
2. "上一话"按钮（有上一章节时启用）
3. "下一话"按钮（有下一章节时启用）
4. 点击 → 直接切换章节

## 测试建议

### 测试用例1：章节列表完整性
1. 下载一个超过6话的漫画（例如10话）
2. 进入下载中心，展开该漫画
3. 验证所有10话都显示在列表中
4. 验证每一话都可以点击进入详情页

### 测试用例2：本地阅读章节切换
1. 下载一个多话漫画，确保至少3话都下载完成
2. 进入任意一话的本地阅读
3. 点击画面中央呼出工具栏
4. 验证左侧"章节"按钮可点击（非灰色）
5. 点击"章节"按钮，验证弹出章节选择对话框
6. 验证对话框中显示所有已完成的章节
7. 选择其他章节，验证成功切换

### 测试用例3：底部导航
1. 在第2话阅读页呼出工具栏
2. 验证底部显示"上一话"和"下一话"按钮
3. 点击"下一话"，验证跳转到第3话
4. 点击"上一话"，验证跳转到第2话

### 测试用例4：边界情况
1. 第1话阅读页："上一话"按钮应禁用
2. 最后一话阅读页："下一话"按钮应禁用
3. 只下载1话的漫画：验证工具栏章节功能正常（虽然只有1个选项）
4. 部分章节未完成：验证只显示已完成的章节

### 测试用例5：兼容性
1. 测试旧版本下载的漫画（没有 parentId 的数据）
2. 测试单话漫画（chapterCount = 1）
3. 测试多话漫画但只下载了部分章节的情况

### 测试用例6：混合场景
1. 同时有在线阅读和本地阅读
2. 在线阅读切换章节（应该正常工作）
3. 切换到本地阅读，验证章节切换依然可用

## 边界修复：缺章时禁止跨章跳转

### 问题现象
- 本地阅读只把 `status == "complete"` 的章节放进 `comicChapterList`。
- 如果已下载第1、2、4话，第3话失败或没有下载记录，那么第2话在“已完成章节列表”里的下一项就是第4话。
- 这会导致点击“下一话”时直接从第2话跳到第4话，跳过实际缺失的第3话。

### 修复方案
- 章节弹窗继续只显示已完成章节，避免用户点击不可阅读章节。
- 底部“上一话/下一话”不再直接使用已完成列表的相邻项。
- 本地阅读额外维护 `LocalChapterNavigationState`，按当前记录的 `chapterIndex - 1` / `chapterIndex + 1` 精确查找原始相邻章节。
- 只有查到相邻章节且其 `status == "complete"` 时才启用按钮；相邻章节失败或不存在时按钮禁用，不跨到更远的已完成章节。

### 验证场景
1. 数据库有第1、2、4话完成，第3话无记录：第2话“下一话”禁用。
2. 数据库有第1、2、3、4话，其中第3话 `status = "error"`：第2话“下一话”禁用。
3. 第1、2、3、4话都完成：第2话“下一话”跳第3话。
## 编译验证

```bash
./gradlew :app:compileDebugKotlin --console=plain
```

✅ 编译成功，无错误

## 相关文件清单

| 文件 | 修改类型 | 说明 |
|-----|---------|------|
| `DownloadListItem.kt` | 修改 | 移除章节显示限制 |
| `DownloadComicDao.kt` | 新增 | 添加按父ID查询章节的方法 |
| `ComicReadViewModel.kt` | 增强 | 添加本地章节加载逻辑 |
| `ComicReadScreen.kt` | 修改 | 在本地阅读时加载章节列表 |

## 问题根因总结

### 问题1：折叠限制
- 硬编码的常量 `MAX_EXPANDED_CHAPTER_ROWS = 6`
- UI 层直接截断列表显示
- 简单但不合理的限制

### 问题2：章节导航失效
- **设计缺陷**：本地阅读和在线阅读共用同一个 `comic` 状态
- **错误假设**：认为本地阅读不需要漫画详情，直接清空
- **连锁反应**：清空详情 → 无章节列表 → UI 功能禁用
- **正确方案**：本地阅读也需要章节列表，但数据来源不同（数据库 vs 网络）

## 架构改进

### 原架构（有问题）
```
在线阅读：网络 → Comic（包含章节列表）
本地阅读：清空 Comic → 无章节列表 ❌
```

### 新架构（已修复）
```
在线阅读：网络 → Comic（包含章节列表）
本地阅读：数据库 → Comic（包含章节列表）✅
```

两种模式都有完整的 `Comic` 对象，只是数据来源不同。

## 未来改进建议

1. **性能优化**：如果章节数量特别多（超过100话），考虑添加虚拟滚动或分页加载
2. **手势支持**：在阅读页添加左右滑动切换章节
3. **快速跳转**：添加进度条式的章节快速跳转
4. **阅读进度**：在章节列表中显示各章节的阅读进度
5. **统一数据模型**：考虑创建统一的章节数据接口，避免在线/本地模式的重复逻辑
6. **预加载优化**：在章节切换时预加载下一章节的图片
