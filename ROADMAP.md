# AudioPlayer 项目进度

## 进行中

- 应用内按钮、列表项和菜单项已共用 300 毫秒点击窗口，首次立即执行。时间边界和跨控件连点单元测试通过，待在设备上验收导航和播放控件连点。

- 播放页已实现原始比例下单指横滑超过窗口宽度的 35% 才翻页，放大后单指或双指可拖动图片。翻页阈值测试通过，待在设备上验收缩放、拖动及图音同步。

- 桌面显示名「何嘉仁学习」、英文字母书本图标和放大的播放控件已打包。已核对 APK 中的应用名和图标资源，待在设备上核对桌面效果及控件尺寸。
- 目录右侧进出、播放页底部进出、播放按钮居中和五档倍速已实现，通过构建、单元测试和 lint，待在设备上验收动画和实际音频速度。
- 播放页标题栏跳转切换、图标控件、书库和目录底部播放栏及目录 `Unit x` 底色已实现，通过构建、单元测试和 lint，待在设备上验证底栏同步、封面导航和跳转模式。
- Android 首版已实现书库导入、同名判重、私有存储和删除，支持按原顺序打开单元、后台播放、按句跳转及图音双向同步。待用《Beyond the Sky》在 Android 14 及以上设备执行 [AGENTS.md](AGENTS.md) 中的验收清单，覆盖系统目录选择器、播放中删除、34.940 秒切图及锁屏和通知栏控件。

## 最近完成

- 2026-11-21 09:37 生成 AudioPlayer 正式签名密钥库和公钥证书，并签署 `1.0` Release APK。签名材料保存在本地 Git 忽略目录，口令由当前 Windows 账户加密保存。
- 2026-11-21 08:32 将 `compileSdk` 升至 37，解决 AndroidX Core `1.19.1` 的编译版本冲突。将《Beyond the Sky》教材纳入版本管理。
- 2026-11-21 00:10 确定书库导入按书名判重，已记录同名拒绝规则和验收场景。
- 2026-11-21 00:02 整理首个 Git 提交，纳入 Android 工程骨架和项目文档。
- 2026-11-20 23:50 确定首版产品和工程规范，记录于 [AGENTS.md](AGENTS.md)。

## 最近验证

- 2026-11-21 21:39 提交前使用 JDK 21 离线复查 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项通过。8 项单元测试通过，新增覆盖 299／300 毫秒边界、忽略点击不延长窗口和不同操作共用窗口。lint 0 项错误、4 项警告，真机触控待验收。

- 2026-11-21 21:12 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项通过。6 项单元测试通过，新增阈值覆盖 35% 边界。lint 0 项错误、5 项已有警告。真机触控待验收。

- 2026-11-21 20:52 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项通过。6 项单元测试通过，覆盖翻页阈值、方向及首末页。lint 0 项错误、5 项已有警告。ADB 无已连接设备，也无已配置模拟器。

- 2026-11-21 20:39 提交前离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项通过，4 项单元测试结果通过。ADB 无已连接设备，真机验收待执行。
- 2026-11-21 09:52 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，构建和 4 项单元测试通过，lint 0 项错误、5 项已有警告。通过 `aapt2` 核对 APK 应用名及自适应图标，通过 `apksigner` 核对调试签名，并检查圆形和圆角方形图标预览。ADB 无已连接设备。
- 2026-11-21 09:38 使用 JDK 21 和 API 37 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，生成调试 APK，4 项单元测试通过，lint 0 项错误、5 项已有警告。ADB 无已连接设备，也无已配置模拟器。
- 2026-11-21 09:37 离线执行 `:app:assembleRelease :app:testDebugUnitTest :app:lintRelease`，构建成功，4 项单元测试通过，lint 0 项错误、4 项警告。用 `apksigner` 和 `zipalign` 验证 Release APK 的 v3 签名、证书指纹和对齐，用 `aapt` 核对包名、版本和启动入口。ADB 无已连接设备，真机安装待验证。
- 2026-11-21 09:21 使用 JDK 21 和 API 37 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，生成调试 APK，4 项单元测试通过，lint 0 项错误、5 项已有警告。ADB 检测到 Android 16 设备，设备上的交互待验收。
- 2026-11-21 08:53 使用 JDK 21 和 API 37 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`，四项均通过。4 项 JVM 测试通过，lint 0 项错误、5 项警告。通过 APK 签名和启动入口检查。ADB 无已连接设备，且无已配置模拟器，设备验收和插桩测试待执行。
- 2026-11-21 08:32 使用 JDK 21 和 API 37 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项均通过并生成调试 APK。检查教材 JSON 的三个单元，图片与时间段一一对应，27 处资源引用均指向现有文件。
- 2026-11-21 00:02 使用 JDK 21 运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，在 `:app:checkDebugAarMetadata` 因 AndroidX Core 与 `compileSdk` 不兼容而中止。单元测试和 lint 未完成，设备未验收。
- 2026-11-20 23:50 用 PowerShell 解析《Beyond the Sky》的目录 JSON，并检查引用文件是否存在。三个单元的图片与时间段分别为 8、6、9 组，封面、章节图片和 MP3 引用均能解析到本地文件。
