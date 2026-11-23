# xiaoai-takeover

接替小米「小爱同学」的 LSPosed 模块 + 配套的 dsh-go 插件。

## 交付物

| # | 交付物 | 形态 | 作用 |
|---|---|---|---|
| 1 | **LSPosed 模块** `io.github.dsh.xiaoai` | APK，注入小爱进程 | 接替小爱的大部分功能 |
| 2 | **dsh-go 插件** `local-xiaoai-bridge` | dsh-go 原生插件 | 承接模块搞不定的事、展示状态 |

## 分工

### 模块接替的部分（替代小爱）

| 能力 | 实现方式 |
|---|---|
| 助理入口 | 接管 `ASSISTANT` 角色与手势/长按 |
| 唤醒词 | 复用小米唤醒引擎，事件转给模块 |
| 语音识别 | hook `OperationManager#onAsrResult` → `getQuery()` |
| 语音播报 | hook `TextToSpeech#speak`，改由我们控制 |
| 压制小爱 | 拦她的应答、弹窗、抢焦点 |
| 简单技能 | 本地用系统 API：时间、闹钟、音量、开关、打开 App、拨号等 |

### 转交 dsh-go 的部分（小爱做不到的）

| 场景 | 为什么转 |
|---|---|
| 推理 / 写代码 / 查资料 | 需要大模型与工具 |
| 操作手机（点按、跨 App 流程） | 需要虚拟屏与自动化 |
| 联网（浏览器、搜索、抓取） | 需要真实网络能力 |
| 长任务 / 需要记忆 | 需要会话与计划 |

### 通道

```
模块（小爱进程）── 本地文件 / 广播 ──► dsh-go 插件（dsh 引擎）
                                        │
                                        └─► 结果回传 → 模块播报/显示
```

先文件后 HTTP：稳定后再换 `127.0.0.1` 的 HTTP 调用。

## 现状

| 项 | 状态 |
|---|---|
| 识别结果 hook 点 | ✅ 已定位（`onAsrResult` → `getQuery()`） |
| 模块 v11 | ✅ 已编译，含 ASR hook + TTS 静音 + 状态文件 |
| dsh-go 插件 | ✅ 状态胶囊 + 面板已装 |
| 静音开关 ↔ 模块 | ✅ 文件握手已通 |
| 入口接管 | ⏳ 待做 |
| 简单技能本地实现 | ⏳ 待做 |
| 请求转交 dsh-go | ⏳ 待做 |

## 仓库结构

```
module/                    LSPosed 模块源码（v11）
plugins/local-xiaoai-bridge/   dsh-go 原生插件
docs/
  XIAOAI-INTERFACES.md     小爱对外接口清单
  re/                      逆向产物（支撑材料，不是交付物）
    L1-L5-inventory.md     组件/权限/库/资源
    cloud-api.md           云端协议模型
    ai-api-domains.md      61 个 API 域
    class-index.csv.gz     152067 类索引
  PLAN.md                  历史计划（顶替阶段的方案，可复用）
```

## 边界

- 只分析本机已安装的应用，用于互操作与自用
- 不转发、不分发小米的模型、资源与商标
- 模块只做最小必要操作，不常驻特权 shell
