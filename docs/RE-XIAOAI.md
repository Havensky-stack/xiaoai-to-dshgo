# 小爱同学完整逆向 · 工作计划

> 目标：把 `com.miui.voiceassist` 逆向到「能自己写一个等价实现」的程度。
> 范围：结构（组件/进程）、契约（接口/协议）、数据流（语音→执行）、资源（模型/配置）、可替换点。

## 零、边界

- 只分析本机已安装的应用，用于互操作与自用改造
- 不转发、不分发小米的模型、资源、商标与私有数据
- 复刻版用自己的包名、图标、名称，不冒充官方

## 一、逆向的六个层次

| 层 | 要回答的问题 | 主要手段 |
|---|---|---|
| L1 结构 | 有哪些组件、跑在哪些进程、谁拉起谁 | 清单解析 + `dumpsys` |
| L2 契约 | 对外暴露什么接口、要什么权限 | 自定义 dex 解析 + jadx |
| L3 数据流 | 一句话从麦克风到执行经过哪些类 | 运行时日志 + hook 打点 |
| L4 状态 | 会话/唤醒/技能的内部状态机 | jadx 读类 + 动态验证 |
| L5 资源 | 模型、配置、词表、UI 资源 | APK 解包 + 二进制分析 |
| L6 能力边界 | 她能做什么、靠什么做（系统权限/服务） | 权限清单 + 调用点 |

## 二、现状（已完成的部分）

### L1 结构

- 569 个组件，135 个对外可见
- 进程：`:core`、`:voice_trigger`、`:provider`、主进程
- 已拿到完整组件表（`/root/workspace/xiaoai/manifest.txt`）

### L2 契约

已确认的对外接口：

| 接口 | 方法 | 权限 | 等级 |
|---|---|---|---|
| `EXTERNAL_AGENT` | openSession/closeSession/submit/… | `EXTERNAL_AGENT` | signature\|privileged |
| `CLI_COMMAND` | executeCommand/executeStreaming/uploadStream | 同上 | signature\|privileged |
| `EXTERNAL_ASR` | startAsr/feedAudio/stopAsr/cancelAsr | 同上 | signature\|privileged |
| `APP_TOOL_PROVIDER` | handleRequest | `BIND_APP_TOOL` | signature |
| `SPEECH_QUERY` | 投一句话 | `QUERY_ENABLE` | — |

CLI 协议：`{id, method, params}` → `{type, code, message, done}`；
method 集合已确认（`ping` / `auth.*` / `session.open` / `agent.submit` / `asr.*`）。

### L5 资源

- 唤醒模型：`assets/wakeupModel/model_wayne_lavender.bin`（2.25 MB）
- 降噪模型：`assets/flexkws/conf/ns_phone_model_*`（final.tflite × 2）
- 唤醒配置：`assets/flexkws/*.json5`（9 个，按平台/麦克风/位深）
- 视觉模型：`assets/model/CVLM-DLA.mnn`

### L6 能力边界

已确认持有的系统能力：助理角色、无障碍、通知监听、来电、账号、蓝牙耳机、多端协同。

## 三、还没做的（按优先级）

### P1 · 语音链路数据流（最关键）

要回答：**一句话到底经过哪些类**。

方法：
1. hook `android.media.AudioRecord.read` 在 `:voice_trigger` 里，确认采样率/声道/帧长
2. hook 小爱的识别回调（先定位类），拿到"最终识别文本"的稳定出口
3. 画出 `麦克风 → VAD → KWS → ASR → NLU → 技能` 的类级调用链

产出：一张带类名的数据流图 + 每段的输入输出契约。

### P2 · NLU 与技能分发

要回答：识别文本之后，她怎么决定"这是查天气还是设闹钟"。

方法：
1. jadx 读 `com.xiaomi.ai.function.*`（已见 `WakeupGuide` 等）
2. 找意图分类入口（关键词表 / 正则 / 云端返回）
3. 找技能注册表（哪个类处理哪个 domain）

产出：意图 → 技能 的路由表。

### P3 · 会话与状态机

要回答：`我在听 → 思考中 → 回答` 这些状态是谁在驱动（我们的 hook 就是被它牵着走）。

方法：读 `VA_UiController` / `VA_QueryRecorder` 等（日志里出现过），找状态常量与迁移。

产出：状态机图。

### P4 · 技能实现清单

要回答：她能做的每件事分别靠什么实现（本地/云端/系统 API）。

方法：按 domain 列技能表，标注实现方式。

产出：能力对照表（用于我们自己的实现优先级）。

### P5 · 资源与模型格式

要回答：模型怎么加载、配置怎么读、词表在哪。

方法：读 `PaddyEngine::Init` 的调用方、json5 的字段含义。

### P6 · 可替换点清单

要回答：哪些地方可以「换成我们的实现」而不破坏系统。

产出：替换点表（hook / 代理 / 自研 三选一）。

## 四、工具与产物

| 用途 | 工具 |
|---|---|
| dex 结构 | 自写 Python 解析器（`/root/workspace/xiaoai/scan.py`） |
| Java 反编译 | jadx（`/root/workspace/jadx/bin/jadx`） |
| native 反汇编 | objdump / Ghidra 12.2（arm64 自编译） |
| 统一分析 | Craft Anything（deep 模式） |
| 运行时 | LSPosed 模块打点 + logcat |

产物统一放 `docs/re/`：
- `L1-components.md` 组件与进程
- `L2-contracts.md` 接口与权限
- `L3-voice-flow.md` 语音数据流（待写）
- `L4-state-machine.md` 状态机（待写）
- `L5-assets.md` 资源与模型
- `L6-capabilities.md` 能力边界
- `REPLACE.md` 可替换点

## 五、里程碑

| 阶段 | 交付 | 判据 |
|---|---|---|
| R1 | 语音链路数据流图 | 能指出每个环节的类名与输入输出 |
| R2 | 意图→技能路由表 | 随便给一句话，能预测她走哪个技能 |
| R3 | 状态机图 | 能解释 `我在听/思考中` 的触发条件 |
| R4 | 能力对照表 | 每项能力标注实现方式 |
| R5 | 可替换点清单 | 每个替换点有验证方案 |
