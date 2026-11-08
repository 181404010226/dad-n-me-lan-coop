# 验证工具

`test-pc.cjs`：两个独立浏览器上下文的原有战斗、900 帧确定性、关卡回调、暂停、重连与失步恢复。

`test-features.cjs`：怒气真实数值与条形显示、耗尽退出、独立结算；通过桌面浏览器触摸协议验证摇杆八方向、死区、多指、取消与失焦，以及三种手机尺寸布局。不是安卓真机测试。

`finish-art.cjs`：对已生成的两个中文标签作等比适配和透明留白，需 Node.js 与 sharp。常规 SWF 构建使用已保存的 `art/imagen/runtime`，无需重新生成图片。

`test-clean-visuals.cjs`：通过实际游戏攻击和死亡流程捕获受击、倒下与结局多个中间帧，输出在 `verification/v030/visuals`。

`swf/build.ps1` 自动调用 `VerifyNonviolent.java`，校验清爽处理前后全部 568 个 ActionScript 脚本块逐字节一致，以及血花路径和死亡碎片已隐藏。

煤气罐修复回归：先运行 `swf/build.ps1`、`app/test/run.ps1` 和 `tools/hazards/build.ps1`，再运行 `node tools/test-hazards.cjs`、`node tools/test-hazard-room.cjs`、`node tools/test-intro-exit.cjs`。这组测试通过独立 QA 桥接摆放原版煤气罐、割草机与首领，触发真实攻击、爆炸和登场回调；包括一号/二号玩家、连锁爆炸、两种首领、强制中断、停滞超时、角色移除、多个锁持有者及退出后正常受伤。真实房间测试使用认证输入及重连回放。QA 桥接不打包进交付游戏。


0.4.0 自动联机验证：`test-auto.cjs` 运行两个独立 Java 原生配对核心和两份独立 Edge 浏览器上下文，模拟 Phone 桥接；验证二号机先开、自动开局、短断线保留引擎、原生活动状态暂停、六帧积压上限、冷重载和增量校验故障恢复。UDP 探测目的地使用电脑回环地址，不冒充 Android 无线广播或 NSD 实测。

`test-performance.cjs` 运行不同输入节奏的独立引擎，记录逐步/增量扫描耗时，并比较所有滚动校验及最终完整状态。属于桌面样本；增量扫描是跨帧窗口检查，不是完整同帧快照。

旧回归工具可通过环境变量 `VERIFY_VERSION=v040` 将本轮证据写入新目录。`test-hazards.cjs` 当前使用快速开局和增量检查，验证煤气罐修复与新版引擎路径兼容；联机 QA 工具使用自动配对客户端、快速开局、增量校验、限速与冷重载回放，核对双方的完整状态。旧手动界面的工具保留为 `test-hazard-room-legacy.cjs`。Android NSD 和手机触摸/性能仍需真机测试。

本轮旧手动界面专项在点击暂停时超时，记录于 `verification/v040/legacy-hazard-room-timeout.log`；未将这项失败算作通过。该界面不在新版 APK 内，同一煤气罐场景在本次自动联机客户端上通过，完整记录见 `hazard-room.json`。


0.4.1 单包模式：`app/test/UnifiedPairTest.java` 验证同一 AUTO 配置、同一协议的两份配对核心，在同时启动、低序设备先启动、高序设备先启动时均只选出一个房主；检查第三设备、出站配对预留、防止同时充当双方房主、原生活动状态、重置与新会话恢复。测试 UDP 分别绑定电脑回环地址 127.0.0.2/3/4，不依赖两个不同 APK 角色。

当前 `test-auto.cjs` 与 `test-hazard-room.cjs` 均使用 AUTO 模式，输出在 `verification/v041/`。游戏引擎、输入与限速策略保持 v040；本轮重跑自动角色分配后的多指、独立操作、六帧上限、短断线、冷重载、增量校验故障恢复和煤气罐实际联机场景。Android NSD 与真实 Wi-Fi 仍未真机验证。


0.5.0：当前 APK 使用单一房主规则与本地显示库。`test-authority.cjs` 覆盖规则角色隔离、状态一致、慢显示不拖房主、显示信用、搭档冷恢复、房主规则回放、显示流更换与缓存重复帧确认；`test-authority-hazards.cjs` 在该模式下触发真实 P2 爆炸与首领动画。`test-visual-rules.cjs` 与归档 0.4.1 做 900 帧完整因果状态比较，并绘制原素材、转发本地音效。`test-presentation-buffer.cjs` 验证无预测插值和包隔离。历史 `test-auto.cjs` / `test-hazard-room.cjs` 对应双模拟，不再作为本轮权威模式的验收入口。当前所有手机性能结论仍需真机验证。
