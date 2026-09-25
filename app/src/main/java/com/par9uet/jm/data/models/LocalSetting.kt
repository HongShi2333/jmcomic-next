package com.par9uet.jm.data.models

const val COMIC_API_SOURCE_BUILTIN = "builtin"
const val COMIC_API_SOURCE_NETWORK = "network"
const val COMIC_API_SOURCE_MIXED = "mixed"
const val APP_LOCK_TYPE_PASSWORD = "password"
const val APP_LOCK_TYPE_PATTERN = "pattern"
const val APP_LOCK_METHOD_BIOMETRIC = "biometric"
const val APP_LOCK_RULE_ANY = "any"
const val APP_LOCK_RULE_REQUIRED = "required"
const val CACHE_INTEGRITY_CHECK_OFF = "off"
const val CACHE_INTEGRITY_CHECK_PARTIAL = "partial"
const val CACHE_INTEGRITY_CHECK_FULL = "full"
const val COVER_CACHE_MAX_DURATION_HOURS = 72
const val DEFAULT_COVER_CACHE_DURATION_HOURS = 24

data class BlockedTagTemplate(
    val name: String = "",
    val tagList: List<String> = listOf(),
)

data class LocalSetting(
    val comicApiSourceList: List<String> = listOf(
        COMIC_API_SOURCE_BUILTIN,
        COMIC_API_SOURCE_NETWORK,
        COMIC_API_SOURCE_MIXED,
    ),
    val comicApiSource: String = COMIC_API_SOURCE_BUILTIN,
    // Empty means the app-private default cache directory; otherwise this is a persisted SAF tree URI.
    val downloadTreeUri: String = "",
    // 偏好推荐开关：开启后将请求网络 API 获取基于登录账号的个性化推荐，可能不稳定
    val preferenceRecommendEnabled: Boolean = false,
    val apiList: List<String> = listOf(
        "https://www.cdnhth.club",
        "https://www.cdnmhwscc.vip",
        "https://www.jmapiproxyxxx.vip",
        "https://www.cdnxxx-proxy.xyz",
        "https://www.jmeadpoolcdn.life"
    ),
    val api: String = apiList[0],
    // DoH is off by default. It can be started manually or restored on app startup.
    val dohEnabled: Boolean = false,
    val dohAutoStart: Boolean = false,
    val dohServerId: String = "tencent",
    val dohCustomServerName: String = "",
    val dohCustomServerUrl: String = "",
    // Include user-installed device CA certificates when validating the DoH endpoint.
    val dohUseDeviceCertificates: Boolean = true,
    val dohPreferIpv6: Boolean = false,
    val themeList: List<String> = listOf(
        "auto",
        "light",
        "dark",
    ),
    val theme: String = "auto",
    val shunt: String = "1",
    val shuntList: List<String> = listOf(
        "1",
        "2",
        "3",
        "4",
    ),
    // 阅读页预先加载的图片张数
    val prefetchCount: Int = 3,
    // scroll || page || tap
    val readMode: String = "scroll",
    // default || side
    val readTapMode: String = "default",
    // 漫画卡片副标题：关闭时显示作者，开启时显示 JM 漫画编号
    val showComicIdAsSubtitle: Boolean = false,
    // 记住上次打开的章节和阅读页，关闭后每次从选定章节第一页开始
    val comicReadingMemoryEnabled: Boolean = true,
    // 记住已读章节并在章节选择器中显示已读状态
    val chapterReadingMemoryEnabled: Boolean = true,
    // 阅读页是否使用实体音量键翻页，默认关闭
    val volumeKeyPageTurningEnabled: Boolean = false,
    // 封面缓存复用时长：0 表示不缓存，最高三天。
    val coverCacheDurationHours: Int = DEFAULT_COVER_CACHE_DURATION_HOURS,
    val launcherDisguise: String = "default",
    // 胁迫页木鱼累计次数及其反馈开关。
    val woodenFishCount: Long = 0L,
    val woodenFishSoundEnabled: Boolean = true,
    val woodenFishVibrationEnabled: Boolean = true,
    // 应用锁通过后是否展示首页预加载过渡。
    val splashLoadingEnabled: Boolean = true,
    val showComicScrollReadTip: Boolean = true,
    val showComicPageReadTip: Boolean = true,
    val showComicCacheNotification: Boolean = true,
    val showComicCacheNotificationName: Boolean = true,
    // 缓存路径迁移到后台后是否显示持续进度通知。
    val showCacheMigrationNotification: Boolean = true,
    val showAiEntry: Boolean = false,
    // AI 模型选择：unlimitedai 或 notrack-ai。
    val aiModel: String = "unlimitedai",
    val blockedTagList: List<String> = listOf(),
    val blockedTagTemplateList: List<BlockedTagTemplate> = listOf(),
    val appLockEnabled: Boolean = false,
    // 密码锁：空字符串表示未设置
    val appLockPassword: String = "",
    // 密码长度 4-8 位
    val appLockPasswordLength: Int = 4,
    // 图案锁：空字符串表示未设置（点序号拼接，例如 "01246"）
    val appLockPattern: String = "",
    // 解锁模式："password" | "pattern" | "both"
    val appLockUnlockMode: String = APP_LOCK_TYPE_PASSWORD,
    // 生物识别仅作为快捷解锁；密码或图形始终保留为保底方式
    val appLockBiometricEnabled: Boolean = false,
    val appLockFingerprintEnabled: Boolean = false,
    val appLockFaceEnabled: Boolean = false,
    // any：任一已启用方式通过；required：必须通过 requiredMethods 中的全部方式
    val appLockUnlockRule: String = APP_LOCK_RULE_ANY,
    val appLockRequiredMethods: List<String> = emptyList(),
    // 胁迫凭据：命中任一已配置凭据后进入电子木鱼页面，不解锁主应用
    val appLockDuressEnabled: Boolean = false,
    val appLockDuressPassword: String = "",
    val appLockDuressPattern: String = "",
    val nsfwWarningDismissed: Boolean = false,
    // 是否已完成首次启动引导
    val onboardingCompleted: Boolean = false,
    // 剪切板自动检测漫画编码：检测到包含数字的文字自动弹出跳转提示，默认关闭
    val clipboardAutoDetectEnabled: Boolean = false,
    // 自动签到：应用启动时若已登录且今日未签到则自动签到，默认关闭
    val autoSignInEnabled: Boolean = false,
    // 推荐源："builtin"（内置 API 推荐）或 "network"（网络 API 推荐）
    val recommendSource: String = "builtin",
    // 调色板预设 ID："default" 表示用主题默认配色，其余为内置预设方案 ID
    val colorPalettePreset: String = COLOR_PALETTE_PRESET_DEFAULT,
    // 自定义四色（ARGB hex 字符串，形如 "#FF4F5F7F"）；null 表示跟随预设
    val customColorPrimary: String? = null,
    val customColorSecondary: String? = null,
    val customColorTertiary: String? = null,
    val customColorError: String? = null,
    // 网格列数：0 表示自适应，2-6 表示固定列数
    val homeGridColumns: Int = 0,
    val collectGridColumns: Int = 0,
    val downloadGridColumns: Int = 0,
    val historyGridColumns: Int = 0,
    val searchGridColumns: Int = 0,
    // 阅读图片内存优化：开启后限制并发解码数并降低解码采样率，缓解低端设备 OOM
    val readMemoryOptEnabled: Boolean = false,
    // 阅读并发解码上限：仅在 readMemoryOptEnabled 开启时生效，推荐值 2
    val readDecodeConcurrency: Int = 2,
    // 缓存完整性检查：off | partial（配置与封面）| full（配置、封面与全部图片页）
    val cacheIntegrityCheckMode: String = CACHE_INTEGRITY_CHECK_OFF,
    // 首页推荐排除标签：带有这些标签的漫画不会出现在首页推荐中
    val homeExcludedTags: List<String> = listOf(),
)

const val COLOR_PALETTE_PRESET_DEFAULT = "default"
const val COLOR_PALETTE_PRESET_OCEAN = "ocean"
const val COLOR_PALETTE_PRESET_SUNSET = "sunset"
const val COLOR_PALETTE_PRESET_FOREST = "forest"
const val COLOR_PALETTE_PRESET_LAVENDER = "lavender"
const val COLOR_PALETTE_PRESET_CUSTOM = "custom"
const val COLOR_PALETTE_PRESET_MONET = "monet"
