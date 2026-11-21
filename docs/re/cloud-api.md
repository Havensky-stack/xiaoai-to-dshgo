# 云端接口目录（AIApiConstants）

> 来源：`com.xiaomi.ai.api.AIApiConstants`（classes5.dex）
> 共 **571 个 API 域**，每个域是一组 String 常量 = 指令名/事件名。

## 一、协议模型：短指令 + 事件流

以 `Nlp` 域为例（40 个常量），可以清楚看出双向结构：

**客户端 → 云端（请求指令）**

| 常量 | 含义 |
|---|---|
| `Request` | 发起一次请求 |
| `ExecuteQuery` | 执行查询 |
| `ConstructRequest` | 组装请求 |
| `StartStream` / `FinishStream` | 流式开始/结束 |
| `StartAnswer` / `FinishAnswer` | 回答开始/结束 |
| `StartPreStream` / `FinishPreStream` | 预流 |
| `StartInstructionGroup` / `FinishInstructionGroup` | 指令组 |
| `ExpectStream` | 期望流式返回 |
| `UploadDialogState` / `UploadClawSession` / `UploadTtsState` | 状态上报 |

**云端 → 客户端（事件）**

| 常量 | 含义 |
|---|---|
| `AnswerResult` | 回答结果 |
| `SemanticCode` | 语义编码 |
| `IntentsWithRelation` | 多意图与关系 |
| `LargeLanguageModelContent` | 大模型生成内容 |
| `TTSFusion` | TTS 融合 |
| `SummaryText` / `DocumentSummary` | 摘要 |
| `LocalStrategy` | 本地策略下发 |
| `RequestWeather` | 天气数据请求 |

这就是**天猫精灵 / Genie 那一类 directive 协议**：客户端发指令，云端回流事件。

## 二、571 个域（部分）

| 域 | 常量数 | 说明 |
|---|---|---|
| `Application` | 86 | 应用控制 |
| `AutoController` | 71 | 车机 |
| `Map` | 41 | 地图导航 |
| `Nlp` | 40 | 语义理解（核心） |
| `General` | 36 | 通用 |
| `MultiModal` | 36 | 多模态 |
| `Phone` | 26 | 通话 |
| `Agent` | 25 | Agent |
| `AudioPlayer` | 23 | 音频播放 |
| `PlaybackController` | 18 | 播放控制 |
| `Alerts` | 17 | 闹钟/提醒 |
| `Dialog` | 17 | 对话 |
| `Internal` | 11 | 内部 |
| `FullScreenTemplate` | 9 | 全屏卡片 |
| `Memo` | 8 | 备忘 |
| `Execution` | 8 | 执行 |
| `DeviceBinding` | 7 | 设备绑定 |
| `Bluetooth` | 6 | 蓝牙 |
| `MiotController` | 5 | 米家 |
| `Launcher` | 5 | 启动器 |
| `LifeService` | 4 | 生活服务 |
| `LLM` | 3 | 大模型 |
| `Offline` | 3 | 离线 |

（完整 571 个域可用 `android_classes_find "AIApiConstants$"` 拉全）

## 三、关键判断：能拿到协议，拿不到授权

| 能拿到 | 拿不到 |
|---|---|
| 指令名与事件名（本文件） | 端点的具体 URL 与签名密钥 |
| 请求/响应的数据类（`com.xiaomi.ai.api.*`） | 设备证书（出厂写入，与账号绑定） |
| 协议形状（directive/event） | 云端策略与技能内容 |

调用链是 `libzrpc2.so` + `libzidl2.so` + `libaivs_jni.so`，
鉴权走小米账号 + 设备证书，**逆向客户端不等于能调云端**。

## 四、这件事的意义

1. 我们知道了小爱的**能力全集**（571 个域），可以据此决定复刻哪几块
2. 我们知道了**协议形状**，自建服务时可以照这个设计
3. 但云端内容服务（天气/音乐/百科）无法复刻，只能换源

## 五、下一步

1. 把 571 个域的名字全量导出成一个列表（用于能力盘点）
2. 挑 3–5 个域深挖数据类（如 `Nlp.Request` / `Nlp.AnswerResult` 的字段）
3. 反编译 `com.xiaomi.ai.api.*` 包，拿到完整的数据模型
