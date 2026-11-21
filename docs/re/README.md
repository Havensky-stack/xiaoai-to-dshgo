# 逆向产物索引

## class-index.csv.gz

152067 个类的全量索引，743 KB（gzip）。

| 列 | 含义 |
|---|---|
| `dex` | 所在 dex（classes5 等） |
| `class` | 全限定类名 |
| `fields` | 字段数 |
| `methods` | 方法数 |

用法：

```bash
zcat class-index.csv.gz | grep 'com.xiaomi.ai.api.AIApiConstants'
zcat class-index.csv.gz | awk -F, '$4>200 {print $2, $4}' | head   # 方法最多的类
zcat class-index.csv.gz | grep -c '^classes'                        # 总行数
```

## 规模

| 项 | 数量 |
|---|---|
| 类 | 152067 |
| 包 | 6080 |
| 最大包 | `com.xiaomi.ai.api`（2541 类） |
| 次大包 | `com.xiaomi.ai.api.intent.domain`（2118） |

## 主要包分布

| 包 | 类数 | 说明 |
|---|---|---|
| `com.xiaomi.ai.api` | 2541 | 云端协议数据模型 |
| `com.xiaomi.ai.api.intent.domain` | 2118 | 意图域定义 |
| `com.aios.osbot.*` | ~2000 | OSBot agent 框架 |
| `com.xiaomi.voiceassistant.*` | — | 主业务 |
| `androidx.compose.material3` | 1426 | UI 框架（非业务） |
| 混淆包（`c82`、`st` 等） | — | 第三方库 |

## 全量反编译

`jadx -d xiaoai_src --no-res --no-debug-info` 在后台跑，产物在本机
`/root/workspace/xiaoai_src`（预计数 GB，不进 git）。

完成后可这样查：

```bash
grep -rn "class Nlp" /root/workspace/xiaoai_src/sources/com/xiaomi/ai/api/ | head
grep -rln "AnswerResult" /root/workspace/xiaoai_src/sources/com/xiaomi/ai/api/ | head
```

## 为什么源码不进 git

- 全量 Java 约 2–4 GB，GitHub 单仓建议 < 1 GB
- 需要时可用同一命令在任意机器重建
- 关键包（`com.xiaomi.ai.api`、`com.xiaomi.voiceassistant`）可在需要时单独导出上传
