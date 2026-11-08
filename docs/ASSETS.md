# 素材和依赖范围

开源许可证覆盖本仓库原创的房间服务、安卓外壳、同步与显示适配器、补丁生成工具和文档；不替原作重新授权。

- **Dad n' Me**：原作由 Tom Fulp、Dan Paladin 创作，音乐由 Dustball 创作。原游戏 SWF、生成的反编译内容及完整改造后 SWF 不在仓库中分发。原作者入口：https://www.newgrounds.com/portal/view/254456 。
- **原始文件准备**：`python tools/fetch-original.py` 从已有项目记录的来源取得原文件，并核对 SHA256。也可使用 `--input <本机原文件>`。构建过程在本机生成导出内容、补丁、游戏和显示库；这些目录均被 Git 忽略。
- **中文与清爽显示补丁图片**：`art/imagen/runtime`、`art/nonviolent/runtime` 是既有适配工作的替换素材，保留原游戏元素的权利归属，属于修改素材，不作原作素材的 MIT 授权声明。
- **Ruffle**：随附运行时采用其 MIT/Apache 双重许可，许可证与二进制一起保存在 `app/assets/ruffle/`；上游 https://github.com/ruffle-rs/ruffle 。
- **QR 编码器**：MIT，见 `licenses/node-qrcode-MIT.txt`。历史扫码外壳保留，当前安卓主流程采用自动配对。
- **JSON-java**：`app/lib/json.jar` 为 Java 房间核心的 JSON 依赖；上游 https://github.com/stleary/JSON-java 。
- **FFDec/JPEXS**：GPL 外部构建工具，不随本仓库分发。使用 26.4.0，设置 `FFDEC_JAR` 后构建。上游 https://github.com/jindrapetrik/jpexs-decompiler 。

构建需要用户自行取得上述外部工具和原游戏文件。APK/完整游戏资源未作为本次源码开源的附件重新托管。
