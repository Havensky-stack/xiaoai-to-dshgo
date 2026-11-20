# xiaoai-to-dshgo

把一台小米手机变成 **Agent 手机**：装一个 APK，给它 root + LSPosed 权限，它顶替小爱，
你动口、它动手。不依赖额外安装 dsh-go。

## 现状

| 能力 | 状态 |
|---|---|
| 抢系统助理角色（`ASSISTANT`） | ✅ 已在小米 Android 17 上验证 |
| 可见的语音界面（`AssistantActivity`） | ✅ |
| 语音识别 | ✅ 走系统 `SpeechRecognizer`（后端 `com.xiaomi.mibrain.speech`） |
| 把识别结果交给 DSH | ✅ `ACTION_SEND` → `dsh_app/.share.ShareActivity` |
| TTS 播报 | ⬜ |
| Agent 循环（参考 `@deepseek-ai/dsh`） | ⬜ |
| 工具层（root shell / Intent / 读屏） | ⬜ |
| 后台屏（虚拟屏）操作 | ⬜ |
| 唤醒词 + 常驻 | ⬜ |
| LSPosed 封印小爱 | ⬜（原型见 `xiaoai-bridge`） |

## 架构

```
        ┌──────────── 入口 ────────────┐
        │ ASSISTANT 角色 / 手势 / 唤醒词 │
        └──────────────┬───────────────┘
                       ▼
        ┌──────────── AssistantActivity ────────────┐
        │  录音 → ASR → Agent 循环 ⇄ 工具层 → TTS    │
        └───────┬───────────────┬───────────────────┘
                │               │
        ┌───────▼──────┐  ┌─────▼──────────────────┐
        │  LLM (HTTP)  │  │ su shell / Intent /    │
        │ OpenAI 兼容   │  │ 无障碍 / 虚拟屏(scrcpy) │
        └──────────────┘  └────────────────────────┘
```

Agent 循环与工具契约**对齐 dsh 的设计**，见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 构建

不依赖 Gradle / Android SDK：`aapt` (v1) + `r8` 的 D8 直接产物。

```bash
cd app
# 需要：android-30 的 android.jar、r8.jar、JDK、aapt、apksigner、zipalign
./build.sh          # 产出 dsh-voice.apk
```

工具链说明见 `docs/TOOLCHAIN.md`（待补）。

## 路线

1. **M1** 壳连本机引擎（127.0.0.1:3080），打通「说→答→念」
2. **M2** 工具层：`su`、Intent、读屏；能执行「关 WiFi」「设闹钟」
3. **M3** 后台屏：打包 scrcpy-server，虚拟屏上操作 App，不抢主屏
4. **M4** 唤醒词（sherpa-onnx KWS）+ 常驻
5. **M5** LSPosed 封印小爱、接管长按

## 许可

MIT
