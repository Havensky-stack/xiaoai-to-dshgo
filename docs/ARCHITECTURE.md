# 架构：把 dsh 的 Agent 设计搬进单 APK

目标：不依赖 Node / dsh-go 运行时，在 Android 进程内实现一个**同构**的 Agent 循环。
契约对齐 `@deepseek-ai/dsh` 的 `dsh-tools` + `dsh-agent-loop`，方便以后与宿主互操作。

## 1. 工具契约（对应 `defineTool`）

dsh 的工具定义包含：名称、描述、参数 schema、规范输出声明、`execute` 主体；
注册表把 schema 自动送进系统提示词；执行前校验参数，无效输入变成普通错误结果。

Java 侧同构：

```java
public interface Tool {
    String name();                 // 面向模型的名称，如 "shell"
    String description();          // 什么时候用、什么时候别用
    JSONObject parameters();       // JSON Schema（object 根）
    ToolResult execute(JSONObject args, ToolContext ctx) throws Exception;
}
```

`ToolResult` 只返回结构化值（`ok` / `data` / `error`），**不返回自然语言**——
措辞交给模型，和 dsh 一样。

注册表：`ToolRegistry.register(Tool)`；`list()` 提供给模型的是 `{name, description, parameters}`。

## 2. 策略（对应 dsh 的 allow / deny / ask）

每个工具声明一个风险级别：

| 级别 | 含义 | 行为 |
|---|---|---|
| `SAFE` | 只读（读屏、查状态） | 直接执行 |
| `ASK` | 有副作用（点按、Intent） | 弹确认卡 |
| `DANGER` | 不可逆（删文件、改系统属性） | 二次确认 + 记日志 |

策略在 `ToolRegistry` 里统一执行，工具实现不关心审批。

## 3. Agent 循环（对应 `dsh-agent-loop`）

```
messages = [system, user]
loop (最多 N 步):
    resp = llm.chat(messages, tools)
    if resp.tool_calls empty: return resp.content
    for call in resp.tool_calls:
        result = registry.execute(call)
        messages.append(tool_result)
```

约束（照抄 dsh 的取舍）：

- 工具结果有长度上限，超了截断并标注
- 同一工具同一参数连续重复 → 提醒而不是硬拒
- 循环有步数上限，超了返回当前进展

## 4. 消息与协议

LLM 走 **OpenAI 兼容** `POST {baseUrl}/chat/completions`：

```json
{
  "model": "...",
  "messages": [...],
  "tools": [{"type":"function","function":{"name":"shell","description":"...","parameters":{...}}}]
}
```

这样 DeepSeek / 通义 / 硅基流动 / 本地 ollama 都能接。

## 5. 工具清单（按里程碑）

| 工具 | 里程碑 | 风险 |
|---|---|---|
| `phone_info` 设备/版本/前台应用 | M2 | SAFE |
| `shell` 执行 `su -c` | M2 | DANGER |
| `intent` 闹钟/日程/导航/拨号/短信/分享 | M2 | ASK |
| `ui_screen` 读当前屏文字 | M2 | SAFE |
| `ui_tap` 点按文字或坐标 | M2 | ASK |
| `vdisplay_*` 后台屏（建屏/投 App/OCR/点击） | M3 | ASK |

## 6. 语音链路

```
AssistantActivity
  → SpeechRecognizer（系统，中文）
  → 文本进 Agent 循环
  → 回答交给 TTS 播报并显示
```

后续 M4 换成 sherpa-onnx：离线 ASR + 关键词唤醒，去掉对系统 ASR 的依赖。

## 7. 权限与安全

- 需要 `su`：所有 `shell` 调用都走 `Runtime.exec(new String[]{"su","-c",cmd})`
- 需要 LSPosed：只用于**封印小爱**（hook 其入口），不用于提权
- 不静默发消息、不静默支付；`DANGER` 级工具必须二次确认
