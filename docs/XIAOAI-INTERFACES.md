# 小爱同学（com.miui.voiceassist 8.2.65）系统接口清单

逆向结论：小爱对外暴露的核心不是语音，而是 **AIOS OSBot** 这套 agent 协议。
它才是「小爱能调的接口」的真身。

## 1. 对外 Service（导出且有 action）

| Action | 实现类 | 权限 | 等级 |
|---|---|---|---|
| `com.aios.osbot.action.EXTERNAL_AGENT` | `.ExternalAgentService` | `com.aios.osbot.permission.EXTERNAL_AGENT` | signature\|privileged |
| `com.aios.osbot.action.CLI_COMMAND` | `.CliCommandService` | 同上 | signature\|privileged |
| `com.aios.osbot.action.EXTERNAL_ASR` | `.ExternalAsrService` | 同上 | signature\|privileged |
| `com.aios.osbot.action.PENDING_INTENT_JUMP` | `.PendingIntentJumpService` | 同上 | signature\|privileged |
| `com.aios.osbot.action.APP_TOOL_PROVIDER` | `UIAgentServiceForOSBot` | `com.aios.osbot.permission.BIND_APP_TOOL` | signature |
| `xiaoai.intent.action.SPEECH_QUERY` / `com.miui.voiceassist.query` | — | `com.miui.voiceassist.QUERY_ENABLE` | — |
| `android.service.voice.VoiceInteractionService` | 助理服务（标准） | `BIND_VOICE_INTERACTION` | signature |

## 2. AIDL 方法签名

```
IExternalAgentService   openSession, closeSession, submit,
                        openFloatWindow, closeFloatWindow, isFloatWindowVisible
IAppToolProvider        handleRequest
IExternalAsrService     startAsr, feedAudio, stopAsr, cancelAsr
ICliCommandService      executeCommand, executeStreaming, executeStreamingWithFd,
                        openUploadStream, sendUploadChunk, endUploadStream, cancelSubscription
IOSbotMessenger         sendNotification
IFileExchange, IPendingIntentJumpService, IExternalRecorderService …
```

含义：

- **IExternalAsrService**：把外部音频喂给小爱的 ASR（可以反过来用小爱的识别能力）
- **IExternalAgentService**：向小爱提交内容/会话，能在它的悬浮窗里渲染
- **ICliCommandService**：一条命令通道，带流式返回
- **IAppToolProvider**：App 向 agent 暴露工具（`handleRequest` 单入口）

## 3. 为什么不能直接调

四个关键权限全是 `signature` 或 `signature|privileged`，第三方 APK 拿不到。
系统里唯一天然持有它们的是小米自家签名应用与 `com.android.shell`。

## 4. 三条可走路（按风险排序）

| 路径 | 做法 | 风险 |
|---|---|---|
| **A. LSPosed 绕过** | 在 `com.miui.voiceassist` 进程内 hook 权限校验，对本包返回 GRANTED | 低，可撤销；依赖模块常驻 |
| **B. packages.xml 授权** | root 改写 `/data/system/packages.xml` 给本包补签名权限 | 高，写坏会无法开机 |
| **C. 特权安装** | 把 APK 放进 `/system/priv-app` | 不可行：bootloader 锁、`/` 是 erofs 只读 |

结论：走 **A**。这也是本项目里 LSPosed 的唯一正当用途——
不是为了提权（root 已经有了），而是为了**冒充系统签名应用去调小爱的接口**。

## 5. 对「Agent 手机」的意义

拿到这层之后，两件事变得可能：

1. **用小爱的能力**：`IExternalAsrService` 直接借它的识别，`ICliCommandService` 借它的命令通道
2. **被小爱的生态调用**：实现 `IAppToolProvider`，让小米自家入口（负一屏、系统设置、超级岛）把我们的工具当成小爱的工具来用

即：不必二选一「替换 or 共存」，可以做到**壳是新的、能力面沿用系统的**。
