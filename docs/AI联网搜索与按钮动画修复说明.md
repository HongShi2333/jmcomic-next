# AI 联网搜索与手表按钮动画修复说明

## 修复范围

本次针对 `jm-mobile-clock` 手表端处理两个问题：

1. AI 页面按钮按下时看不到缩放反馈。
2. AI 联网搜索在部分网络、DoH 或搜索引擎返回拦截页时没有结果，导致 AI 无法获得联网上下文。

## 按钮动画

手表端 AI 页面底部保留两枚 48dp 圆形按钮：

- 写消息 / 停止生成
- 对话工具

按钮现在使用独立的 `MutableInteractionSource`，并通过 `watchPressFeedback` 在按下时缩放至约 `0.94f`，使用弹簧动画恢复。打开写消息或工具面板时延迟约 90ms，确保用户能看到按下反馈，而不是点击后立即被新页面覆盖。

## 联网搜索修复

### 1. DoH 失败自动回退

搜索请求先使用应用配置的 DoH DNS；如果解析或请求失败，会自动使用系统 DNS 的备用 `OkHttpClient` 重试。

### 2. DuckDuckGo 双入口

默认无 Key 搜索仍使用 DuckDuckGo：

- 优先请求 HTML 搜索接口；
- HTML 接口返回拦截页、同意页或解析不到结果时，自动切换 Lite 接口；
- 放宽结果链接属性顺序解析；
- 自动还原 DuckDuckGo `uddg` 跳转链接。

### 3. 指定引擎失败时自动降级

用户指定 Bing、Sogou、Baidu、SearXNG 或 Tavily 后，如果该引擎暂时返回空结果，会自动尝试无 Key 的自动搜索链路，避免一次引擎异常直接导致 AI 没有联网结果。

### 4. 搜索上下文处理

成功搜索后仍会把标题、摘要和来源链接拼接为系统消息，交给 AI 优先参考；搜索失败时会明确告诉 AI“没有获得可用联网结果”，避免把旧知识伪装成最新信息。

## 涉及文件

- `jm-mobile-clock/src/main/java/com/par9uet/jm/ui/screens/WatchAiChatScreen.kt`
- `jm-mobile-clock/src/main/java/com/par9uet/jm/ui/components/WatchMotion.kt`
- `jm-mobile-clock/src/main/java/com/par9uet/jm/repository/AiChatRepository.kt`

## 验证方式

已执行轻量验证：

```text
:jm-mobile-clock:compileDebugKotlin
:jm-mobile-clock:compileDebugAndroidTestKotlin
:jm-mobile-clock:testDebugUnitTest
:jm-mobile-clock:compileReleaseKotlin
```

未执行 `assemble`、`build`、`bundle` 或 `clean`。

## 使用建议

1. AI 设置中的联网搜索开关打开后直接发送问题。
2. 不配置 Tavily Key 时使用“自动”即可。
3. 如果某个自定义 SearXNG 实例不可用，自动模式会回退到 DuckDuckGo 和国内搜索引擎。
