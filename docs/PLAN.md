# Agent 手机 · 计划书

> 版本 2026-11-21，基于本机实测记录（KernelSU + LSPosed/Vector + 小米 Android 17）

## 一、目标

**一句话**：把一台小米手机变成 Agent 手机——装一个 APK，给它 root 与 LSPosed 权限，
它顶替小爱，能听、能答、能在后台替你操作手机，不依赖额外安装 dsh-go。

**三条硬指标**

| # | 指标 | 判据 |
|---|---|---|
| 1 | 单 APK 自足 | 新手机只需装 APK + 给 root/模块权限，即可工作 |
| 2 | 顶替小爱 | 唤醒与入口归我们，小爱的响应被封印 |
| 3 | 后台操作 | AI 操作 App 时，用户主屏、输入法、焦点不受影响 |

## 二、现状盘点

### 已验证（有实机证据）

| 能力 | 证据 |
|---|---|
| 抢系统助理角色 | `io.github.dsh.voice` 持 `ASSISTANT`，`connected` 成功 |
| 可见语音界面 + 系统 ASR | 日志出现识别结果「你好啊」 |
| 签名权限绕过 | `DSHPERM: granted …EXTERNAL_AGENT uid=10055` |
| 绑定小爱 CLI 并调用 | 事务码 1/2/4/5 全部响应，`session.open` 成功 |
| 原生引擎可在自建 APK 内加载 | `version=flexkws_Release_git_2026-08-24…` |
| 手机自编 Android .so | NDK sysroot + clang `-nostdlib` → ELF64 AArch64 |
| 逆向环境 | jadx 可用；Ghidra 12.2 arm64 反编译器自编译；Craft doctor healthy |

### 关键发现（决定路线）

1. **`libwakeup-engine.so` 的 JNI 符号完整**：
   `Java_com_xiaomi_asr_engine_jni_WakeupEngineInterface_wakeupInit/wakeupFeedData/wakeupGetFrameSize/wakeupReset/wakeupDestroy`
   → **不需要写 JNI shim**，Java 侧建同名类即可绑定。
2. `wakeupInit()` **不收参数**（JNIEnv 被忽略），内部调
   `PaddyWakeup::PaddyEngine::Init(gWakeupDecoder, "<0x52a55 字符串>", 1)`。
3. `flexkws` 的 `init` 收的是**配置目录**（读 `*.json5`），不是模型文件——
   之前的两次崩溃就是传错了东西。
4. 小爱 APK 内含 9 个 `assets/flexkws/*.json5` 与 `assets/wakeupModel/model_wayne_lavender.bin`。
5. 上传流可绕开 CLI 的连接亲和性，但 chunk 报文格式未定。

### 未验证（下一步就要打掉的）

- `libwakeup-engine.so` 的依赖链是否完整（是否还缺 `libpaddy*.so`）
- `wakeupInit` 加载的资源路径（`0x52a55` 字符串内容）
- 唤醒引擎是否校验调用方签名/包名
- 后台屏在 Android 17 上能否建（`ADD_TRUSTED_DISPLAY` 已确认存在）

## 三、阶段计划

### P0 · 唤醒引擎打通（当前）

| 项 | 内容 |
|---|---|
| 做什么 | 读 `0x52a55` 字符串；把 `libwakeup-engine.so` 及其依赖打进测试 APK；建 `com.xiaomi.asr.engine.jni.WakeupEngineInterface` 类；调 `wakeupInit()` → `wakeupGetFrameSize()` → 喂 PCM → 看 `wakeupFeedData` 返回 |
| 验收 | 日志出现 `wakeupInit()=0`，且 `wakeupGetFrameSize()>0`；播放「小爱小爱」录音能触发命中 |
| 风险 | 符号可见但引擎内部依赖小爱进程环境（证书/路径）→ 失败则退回 `flexkws` 路线 |

### P1 · 语音闭环

| 项 | 内容 |
|---|---|
| 做什么 | 唤醒 → 录音 → ASR → Agent 引擎 → TTS 播报；ASR 先用系统 `SpeechRecognizer`，稳定后换本地方案 |
| 验收 | 说一句「今天天气怎么样」，手机念出回答，全程无需触摸 |
| 风险 | 系统 ASR 依赖小米服务；本地方案需要模型体积与延迟的权衡 |

### P2 · 工具层

| 项 | 内容 |
|---|---|
| 做什么 | `shell`（su）、`intent`（闹钟/导航/电话）、`ui_screen`（读屏）、`ui_tap` |
| 验收 | 「把 WiFi 关掉」「设一个 7 点闹钟」「打开微信」全部成功 |
| 风险 | 危险工具需要确认卡；自绘界面读屏可能拿不到文字 |

### P3 · 后台屏

| 项 | 内容 |
|---|---|
| 做什么 | 打包 scrcpy-server，建虚拟屏（`FLAG_OWN_CONTENT_ONLY` + `FLAG_OWN_FOCUS`），在虚拟屏上启动 App 并截图/点击 |
| 验收 | 主屏放着视频，AI 在虚拟屏完成一次「打开设置→找到 WLAN→读出状态」 |
| 风险 | Android 17 上 `createNewVirtualDisplay` 行为可能与 Android 16 不同；3D 应用需要 HWC |

### P4 · 常驻与封印

| 项 | 内容 |
|---|---|
| 做什么 | 开机自启（KernelSU `service.d`）；LSPosed 模块让小爱不响应唤醒/入口；长按电源键改绑 |
| 验收 | 重启后直接「小爱小爱」唤起的是我们；小爱 App 不出声、不弹窗 |
| 风险 | HyperOS 可能把长按键硬绑给小爱，需要 hook SystemUI 手势层 |

### P5 · 打包与分发

| 项 | 内容 |
|---|---|
| 做什么 | 合并为单 APK；首次启动解压模型；配置页填 API；升级路径（引擎版本与壳解耦） |
| 验收 | 一台干净的小米手机：装 APK → 给 root/模块权限 → 一句话可用 |
| 风险 | APK 体积（模型 + 引擎）与首次启动耗时 |

## 四、关键技术决策（已定）

| 决策 | 结论 | 理由 |
|---|---|---|
| 唤醒引擎 | **优先用 `libwakeup-engine.so` 的 JNI 直连** | 符号完整、官方接口、无需 shim |
| 唤醒备选 | `flexkws`（配置目录 + json5） | 已确认契约，但需自带 9 个 json5 |
| 原生编译 | NDK sysroot + 本机 clang + `-nostdlib` | NDK 无 arm64 宿主工具链，只能借用 sysroot |
| Agent 形态 | 壳薄、引擎厚：能内嵌 dsh 引擎就内嵌，否则先连本机引擎 | 插件兼容与升级维护 |
| 小爱处理 | **停用而非删除** | 删除可能影响系统入口，停用可逆 |

## 五、风险清单

| 风险 | 影响 | 应对 |
|---|---|---|
| 唤醒引擎依赖小爱签名环境 | P0 失败 | 退回 flexkws；再不行改用 sherpa-onnx 自训练 |
| HyperOS 长按键不可改绑 | 入口受限 | 保留悬浮球 + 通知常驻作为入口 |
| Android 17 虚拟屏限制 | P3 失败 | 退回无障碍 + 前台操作（牺牲「不抢屏」） |
| 内嵌引擎跑不起来 | 单 APK 目标受损 | 降级为「壳 + 本机引擎」，功能不受损 |

## 六、需要你拍板

1. **模型接入方式**：APK 内置配置页填 OpenAI 兼容接口（默认），还是要连你现有的 dsh 引擎？
2. **唤醒词**：继续用「小爱小爱」（复用官方模型），还是换自定义词（需要另训/另采模型）？
3. **优先级**：P0 唤醒引擎 vs P1 语音闭环，先要哪个能用的东西？

## 七、节奏预估

| 阶段 | 预估 |
|---|---|
| P0 | 1–2 轮（读字符串、打测试 APK、实机验证） |
| P1 | 2–3 轮 |
| P2 | 3–4 轮 |
| P3 | 3–5 轮（不确定性最高） |
| P4 | 2–3 轮 |
| P5 | 2 轮 |

> 说明：轮次按「一次可验证的迭代」计，不含等待下载/重启的时间。
