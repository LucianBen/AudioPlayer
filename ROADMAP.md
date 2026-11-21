# AudioPlayer 项目进度

## 待实现

- 按 [AGENTS.md](AGENTS.md) 实现 Android 首版，完成书籍导入和删除、单元目录、后台播放、按句跳转及图音同步。

## 最近完成

- 2026-11-21 08:32 将 `compileSdk` 升至 37，解决 AndroidX Core `1.19.1` 的编译版本冲突。将《Beyond the Sky》教材纳入版本管理。
- 2026-11-21 00:02 整理首个 Git 提交，纳入 Android 工程骨架和项目文档。
- 2026-11-20 23:50 确定首版产品和工程规范，记录于 [AGENTS.md](AGENTS.md)。

## 最近验证

- 2026-11-21 08:32 使用 JDK 21 和 API 37 离线运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，三项均通过并生成调试 APK。检查教材 JSON 的三个单元，图片与时间段一一对应，27 处资源引用均指向现有文件。
- 2026-11-21 00:02 使用 JDK 21 运行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，在 `:app:checkDebugAarMetadata` 因 AndroidX Core 与 `compileSdk` 不兼容而中止。单元测试和 lint 未完成，设备未验收。
- 2026-11-20 23:50 用 PowerShell 解析《Beyond the Sky》的目录 JSON，并检查引用文件是否存在。三个单元的图片与时间段分别为 8、6、9 组，封面、章节图片和 MP3 引用均能解析到本地文件。
