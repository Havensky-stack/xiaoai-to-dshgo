# xiaoai-re

小米「小爱同学」（`com.miui.voiceassist` 8.2.65）的完整逆向工程记录，
目标是**复刻其中一部分功能、改造另一部分**。

> 早期版本的目标是「用 dsh-go 顶替小爱」——那部分的计划与代码仍保留在
> `docs/PLAN.md` 与 `app/`、`module/`、`plugins/` 中，作为历史与可复用资产。

## 现状

| 项 | 状态 |
|---|---|
| 结构清单（组件/权限/进程/库/资源） | ✅ 完成 |
| 云端接口目录（61 个域） | ✅ 完成 |
| 全量类索引（152067 个类） | ✅ 完成 |
| 协议层源码（`com.xiaomi.ai.api`） | ✅ 反编译完成，2297 个文件 |
| 业务层源码（`com.xiaomi.voiceassistant`） | ✅ 4 个 dex 反编译完成，约 4800 个文件 |
| 语音链路（唤醒→ASR） | ✅ 定位到 `OperationManager.onAsrResult` → `getQuery()` |
| NLU / 技能路由 | ⏳ 待做 |
| 云端调用（鉴权） | ❌ 需要设备证书，不可复刻 |

## 仓库结构

```
docs/
  re/                     逆向产物
    L1-L5-inventory.md    组件/权限/库/资源清单
    cloud-api.md          云端 61 个 API 域与协议模型
    ai-api-domains.md     61 个域的完整列表
    class-index.csv.gz    152067 个类的索引（743 KB）
    README.md             产物用法
  PLAN.md                 早期「顶替小爱」计划书（历史）
  ARCHITECTURE.md         早期架构说明（历史）
  XIAOAI-INTERFACES.md    小爱对外接口清单（仍有参考价值）
  RE-XIAOAI.md            逆向工作的六层拆解与里程碑
app/                      助理壳（历史，可复用）
module/                   LSPosed 模块源码（v11，hook 识别结果）
plugins/                  dsh-go 原生插件（历史）
```

## 关键结论

1. **小爱的智能在云端**：APK 只是客户端，571 个内部类 / 61 个域描述的是
   directive/event 协议，真正的理解与内容服务不在包里。
2. **云端不可复刻**：调用链是 `libzrpc2` + 设备证书 + 小米账号，
   逆向客户端拿不到授权。
3. **可复刻的是形态**：唤醒、入口、交互、设备控制、技能编排。
4. **语音链路的稳定出口**：`com.xiaomi.voiceassistant.instruction.base.OperationManager#onAsrResult`
   → 参数 `t51.b` 的 `getQuery()`——这是识别文本最可靠的获取点。

## 下一步

1. NLU / 技能路由：读 `com.xiaomi.ai.function.*`，建立「意图 → 技能」表
2. 挑 3–5 个可复刻的技能，写出等价实现
3. 需要云端能力的地方，换源（自建模型或第三方 API）

## 边界

- 仅分析本机已安装的应用，用于互操作与自用改造
- 不转发、不分发小米的模型、资源、商标与私有数据
- 复刻版使用自己的包名与标识，不冒充官方
