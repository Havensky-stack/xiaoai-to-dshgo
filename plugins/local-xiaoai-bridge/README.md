# local-xiaoai-bridge（dsh-go 原生插件）

小爱桥的 dsh-go 原生界面：输入框上方的状态条 + 一个面板。

## 装法

```sh
cp -r <repo>/plugins/local-xiaoai-bridge "${DSH_HOME:-$HOME/.dsh}/dsh-go/plugins/"
node "${DSH_HOME:-$HOME/.dsh}/skills/dsh-go-plugin/kit/bin/check.mjs" \
     "${DSH_HOME:-$HOME/.dsh}/dsh-go/plugins/local-xiaoai-bridge" --load
dsh plugin --profile web add "${DSH_HOME:-$HOME/.dsh}/dsh-go/plugins/local-xiaoai-bridge"
```

装上即生效，不用重启引擎（首次装新目录时除外）。

## 界面

| 位置 | 内容 |
|---|---|
| 输入框上方状态条 | `小爱桥：待机` / `小爱桥：转发中` / `小爱桥：小爱静音`，点按切换静音 |
| 面板「小爱桥」 | 助理角色持有者、模块是否加载、最近一次转发文本、静音状态 |

面板在对话详情页的「插件」页签，以及「全部功能」里。

## 数据来源（文件契约）

插件只读 `/sdcard/Download/DSH-Go/` 下的两个文件，文件缺失时显示「尚未上报 / 尚无记录」，
不编造数据。

| 文件 | 谁写 | 内容 |
|---|---|---|
| `xiaoai-mute.txt` | 本插件写（点按切换） | `1` = 静音，`0` = 正常 |
| `xiaoai-bridge-status.json` | LSPosed 模块（待实现写入） | `{"role":"com.miui.voiceassist","moduleLoaded":true,"lastText":"…","lastAt":1795232891000}` |

`lastAt` 是毫秒时间戳；60 秒内有值则状态条显示「转发中」。

## 当前边界

- 静音开关**只写文件**；真正拦小爱 TTS 是 LSPosed 模块 `io.github.dsh.xiaoai`（v4 已内置 hook），
  它读取该文件后才生效——这一步尚未接通。
- 助理角色与模块状态目前靠模块写入 `xiaoai-bridge-status.json`，模块侧写入同样待实现。
