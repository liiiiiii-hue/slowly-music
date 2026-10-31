# Slowly Music

一款以浅色主题、封面和同步歌词为中心的 Android 私人音乐播放器。使用 Kotlin、Jetpack Compose 和 Media3 开发。

当前版本：**0.5.0** · 最低 Android 8.0（API 26） · MIT 开源。

## 功能

- 连接 HTTPS WebDAV，选择音乐目录并递归扫描 MP3；读取标题、歌手、专辑与内嵌封面。
- 自建歌单、编辑封面、搜索歌曲，支持顺序、随机、单曲循环及播放队列。
- 外部 LRC 优先，支持部分 MP3 内嵌同步歌词；点击歌词定位、调整字号、切换封面视图。
- 五种浅色主题、专辑模糊背景；当前歌曲高亮与列表定位按钮。
- 在线音频缓存上限 5 GiB（界面标为 5GB），按最近最少使用策略淘汰。
- 联动手机已安装的 QQ 音乐：暂停、继续、上一首、下一首、打开 QQ，显示对方媒体会话提供的封面与歌词。
- 可选通过 Android Keystore 加密保存 WebDAV 连接。

## 安装与使用

直接下载：[SlowlyMusic-0.5.0.apk](https://github.com/liiiiiii-hue/slowly-music/releases/download/v0.5.0/SlowlyMusic-0.5.0.apk)（约 13.5 MiB，可直接安装）。

[发布说明与 SHA-256 校验文件](https://github.com/liiiiiii-hue/slowly-music/releases/tag/v0.5.0)。安装包沿用此前个人测试版签名，可覆盖安装；首次安装需允许对应来源安装应用。也可按下方步骤自行构建。

完整步骤见 [使用说明书](docs/USER_GUIDE.md)，构建步骤见 [开发与架构](docs/DEVELOPMENT.md)。

```sh
# 安装 JDK 17 和 Android SDK 35 后
./gradlew :lyrics:test :app:assembleDebug
# Windows 使用 gradlew.bat
```

## QQ 音乐联动范围

这是第三方媒体控制功能，与腾讯无隶属关系，也不包含 QQ 曲库或播放授权。需要在系统中授予 Slowly Music 通知访问权限，并先在 QQ 中播放歌曲。源码只查询 QQ 媒体会话，不处理通知正文。

QQ 仍负责播放自己的音频；会员、账号、曲库权限由 QQ 决定。本项目不读取 QQ 收藏歌单、不获取歌曲播放链接，也不实现 QQ 与 WebDAV 混合随机队列。歌词来自非标准字段 `ucar.media.metadata.LYRICS_WHOLE`，是否提供及格式可能随 QQ 版本变化。支持的定位操作取决于对方会话能力。

## 当前限制与验证

这是开发中的个人播放器。0.5.0 已加入前台媒体播放服务、系统媒体通知和后台队列切换，播放与页面生命周期分离；系统强制停止、厂商限制和长期网络故障仍可能中断播放。队列在进程内保存，进程被杀后不自动恢复播放。远程索引尚未使用 Room，暂未实现 ETag 增量同步。当前扫描入口以 MP3 为主，不承诺完整 FLAC 标签和歌词支持。

QQ 暂停/继续/上下首已有用户真机验证；封面在该设备上可读取。0.4.2 修复封面内容未变时反复重建模糊背景的问题，模拟器图片测试通过，修复后的 QQ 真机视觉效果仍待复核。不同 Android、OriginOS 与 QQ 版本需要分别验证。

## 隐私与开源

阅读 [隐私说明](docs/PRIVACY.md)、[第三方组件说明](THIRD_PARTY_NOTICES.md)。仓库不包含账号、密码、签名私钥、个人歌单、音乐或歌词资源。

项目自身代码按 [MIT License](LICENSE) 发布。第三方依赖及 Gradle Wrapper 保留各自许可证。请仅播放你有权使用的音乐。
