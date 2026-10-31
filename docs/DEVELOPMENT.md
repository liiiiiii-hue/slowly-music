# 开发与架构

## 环境

JDK 17、Android SDK Platform 35、Android SDK Build Tools、Gradle Wrapper 8.10.2；Android Gradle Plugin 8.7.3，Kotlin 2.0.21。Android Studio 可打开仓库根目录导入。

在本机 `local.properties` 填写 `sdk.dir=/your/android/sdk`（Windows 可用正斜杠），或设置 ANDROID_HOME。不要提交 local.properties。

```sh
./gradlew :lyrics:test :app:assembleDebug
./gradlew :app:assembleRelease
# 连接设备/模拟器后
./gradlew :app:connectedDebugAndroidTest
```

Windows 使用 `gradlew.bat`。首次构建需要网络下载 Gradle 与 Maven 依赖。APK 位于 `app/build/outputs/apk/`。

当前 release 使用本机 debug signingConfig，适用于开发/个人测试，不是正式商店签名方案。不同开发机器签名不同，不能保证覆盖此前安装；正式发布应配置自己的签名并妥善保管私钥，绝不提交到仓库。

## 目录

- `app/`：Android UI、播放器、WebDAV、持久化、QQ媒体控制。
- `lyrics/`：纯 Kotlin LRC、ID3歌词及标签解析与测试。
- `gradle/`：Wrapper。
- `docs/`：使用、开发和隐私文档。

`PlaybackEngine` 使用应用上下文持有单一 ExoPlayer 和完整队列，Media3 自动切换下一首；`PlaybackService` 提供 mediaPlayback 前台服务、平台 MediaSession、通知及锁屏控制，媒体与网络唤醒锁由 Media3 管理。`MainActivity` 只订阅播放状态，不在销毁时释放播放器；`LibraryScreens` 管理库及歌单界面；`PlayerScreens` 共享封面/歌词与控制组件；`QqMusicScreen` 读取 QQ MediaSession，通过 `QqBridge` 发送标准控制；`ArtworkTools` 预计算模糊背景，`ArtworkRevision` 按封面内容识别更新。

WebDAV 使用 OkHttp；音频使用 Media3 ExoPlayer 及 SimpleCache，LRU上限5GiB。`ConnectionStore` 使用 Android Keystore/AES-GCM 保存连接凭证。

## 验证与限制

发布构建与 lintVital 已通过；0.4.2 在 API35 模拟器运行两项 ArtworkTools 测试，覆盖模糊生成及相同内容的新 Bitmap 不触发封面版本变化。QQ 实际兼容性需要安装官方 App 的真机验证。仓库测试不包含私人歌曲、账号或网络服务。

CI 运行纯 Kotlin 测试和 debug 构建，不等同真机音乐播放验收。欢迎通过 Issue 报告可复现问题，通过 PR 提交小范围修改及对应验证。
