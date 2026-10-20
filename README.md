# 爱美 · Aimei

Android 随身试衣间。原生首页、私人衣橱与 Anywear 官方手机实时试衣页面。

## 安装

在 [Releases](https://github.com/nmm26301-maker/Aimei/releases) 下载最新测试版的 Aimei-0.6.0.apk。
该链接只有工作流成功完成编译、测试、lint 和签名验证后才会出现安装包。

## 使用

1. 安装后点击「开启实时试衣」。
2. Anywear 官方页面中点击 Enable camera 并允许摄像头；若显示介绍页，先点击 Try Example。
3. 使用页面提供的服装进行试穿。若页面有服装上传控件，可选择爱美衣橱或手机相册。
4. 返回首页会结束试衣。浏览器入口可用于排查手机 WebView 兼容问题。

衣橱图片保存在应用私有目录，卸载会清除。画面处理由 Anywear 服务提供，爱美自身不录制视频。
爱美不提供尺码测量，也不保证第三方服务持续可用。

## 构建

Java 17、Gradle 8.9、Android SDK 35。GitHub Actions 自动构建，并执行单元测试、Android lint 和签名验证。
构建命令：gradle assembleDebug testDebugUnitTest lintDebug。

当前安装包采用 Android 测试签名。工作流缓存测试签名，并生成单独的测试签名恢复产物，保留 90 天；不将私钥写入公开源码或发布附件。

## 验证边界

编译、自动化检查和 APK 签名验证不能替代实体手机测试。摄像头授权、网络连接、服装选择和实时换装仍需 Android 真机验证。

## 项目来源

本仓库的 0.6.0 为可直接编译的新工程，未读取或合并先前 v0.5 源码 ZIP。
没有复制 Anywear 页面源码或使用其非公开接口；实时试衣通过官方 HTTPS 页面提供。
