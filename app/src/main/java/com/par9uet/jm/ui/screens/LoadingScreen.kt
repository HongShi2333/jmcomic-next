package com.par9uet.jm.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.par9uet.jm.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 启动加载页展示的功能小 Tips */
private val LOADING_TIPS = listOf(
    "在设置中可以切换内置 API 和网络 API 两种数据源",
    "长按漫画封面可以快速收藏或取消收藏",
    "在阅读设置中可以开启图片内存优化，缓解低端设备卡顿",
    "搜索时可以使用 +标签名 来包含特定标签的漫画",
    "在设置→显示中可以自定义首页、收藏、缓存的网格列数",
    "阅读时点击屏幕中间可以显示/隐藏工具栏",
    "在个人中心可以管理标签排除模板，快速套用筛选条件",
    "设置中可以开启自动签到，省去每天手动签到的麻烦",
    "长按漫画标签可以一键复制标签内容",
    "在设置→调色板中可以自定义应用的主辅颜色",
    "搜索结果页保留了搜索参数，返回编辑不会丢失",
    "收藏夹支持按标签、作者筛选，还支持逻辑门选择",
    "内置 API 模式下可以在设置中开启偏好推荐",
    "阅读图片内存优化默认关闭，出问题时手动开启即可",
    "在设置中可以开启剪切板自动检测，复制漫画编码后自动提示跳转",
    "漫画详情页长按 JM 编码标签可以查看完整信息",
    "应用支持应用锁，可以设置密码或图案保护隐私",
    "设置中可以更改应用图标，伪装成系统工具或相册",
    "AI 对话页支持联网搜索，像 Lobe Chat 一样展示引用来源",
    "在 AI 对话中可以开启深度思考，先思考再回答",
    "人格面具系统允许你自定义 AI 的名字、职业、年龄与简介",
    "AI 对话支持多分支编辑，修改历史消息不影响其他分支",
    "可以一键新建对话，也可以滑动抽屉切换历史对话",
    "AI 联网搜索支持 Tavily、DuckDuckGo、Bing 等多种引擎",
    "在 AI 搜索设置中可以自定义搜索条数与搜索深度",
    "AI 对话支持重试，可以选择重新生成、更详细或更精简",
    "在设置中可以备份本地配置、AI 对话记录和人格面具",
    "备份文件支持密码、图案或密码+图案多重保护",
    "恢复备份时可以选择只恢复部分内容，不影响其他数据",
    "首页推荐支持标签排除，不推送包含特定标签的漫画",
    "下载管理支持暂停/继续，出错或完成的任务不会显示暂停按钮",
    "阅读时左右滑动可以翻页，上下滑动可以连续滚动",
    "漫画缓存通知每 3 秒更新一次进度，避免通知频繁刷新",
    "在设置中可以开启夜间模式，保护夜间阅读体验",
    "应用支持动态取色（Android 12+），自动匹配系统壁纸",
    "收藏夹管理改为点击管理按钮打开底部弹窗，操作更便捷",
    "引导流程会在登录后询问是否开启自动签到",
    "应用启动加载页会轮播功能 Tips，帮助你快速上手",
    "数据备份使用 SHA-256 校验，确保备份文件完整无损",
    "阅读图片解码并发数可调，低端设备建议降低并发数",
    "搜索结果可自定义每行显示的漫画数量（2-6 列或自适应）",
    "历史记录、缓存、收藏夹页面都支持自定义网格列数",
    "AI 对话中的代码块支持语法高亮和一键复制",
    "联网搜索结果会以卡片形式展示，可点击跳转原文",
    "深度思考过程以可折叠卡片展示，展开可查看完整推理"
)

/**
 * 应用启动加载动画页
 *
 * 居中展示应用 Logo、应用名称、进度条和轮播 Tips。
 * 用于应用启动初始化阶段及引导完成后的过渡展示。
 */
@Composable
fun LoadingScreen(
    showSkipButton: Boolean = false,
    onSkip: () -> Unit = {}
) {
    // 初始 tip 随机选取，之后每 3 秒随机切换到不同的 tip（不重复上一个）
    var tipIndex by remember {
        mutableIntStateOf(Random.nextInt(LOADING_TIPS.size))
    }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            if (LOADING_TIPS.size <= 1) break
            var next = tipIndex
            while (next == tipIndex) {
                next = Random.nextInt(LOADING_TIPS.size)
            }
            tipIndex = next
        }
    }

    // 使用 Bitmap 方式加载 mipmap adaptive-icon，避免 painterResource 对 mipmap XML 的不支持
    val logoBitmap = remember {
        runCatching {
            ContextCompat.getDrawable(context, R.mipmap.logo)?.toBitmap()
        }.getOrNull()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // 应用 Logo，圆形裁剪；使用 Bitmap 方式兼容 adaptive-icon XML
                logoBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 应用名称
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (showSkipButton) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onSkip) {
                        Text("跳过")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // 进度条（indeterminate 模式）
                LinearProgressIndicator(
                    modifier = Modifier.width(160.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Tips 轮播区域：固定高度避免文本长度变化导致布局跳动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = tipIndex,
                        animationSpec = tween(durationMillis = 500),
                        label = "tipCrossfade"
                    ) { index ->
                        Text(
                            text = LOADING_TIPS[index],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
