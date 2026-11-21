# 小爱同学 8.2.65 结构清单（Craft Anything 实测）

> 证据来源：`android_package_show`（craft，builtin 引擎，签名已密码学校验）
> 目标 SHA256：`4108e3871269139d36b22aec08921456b5ff8a8b663adabb39c085445a92f7c7`

## 一、包信息

| 项 | 值 |
|---|---|
| 包名 | `com.miui.voiceassist` |
| 版本 | 8.2.65.4714（code 508002065） |
| minSdk / targetSdk | 26 / 33 |
| 组件 | 569 个，其中 **163 个对外可达** |
| 权限 | 声明/使用 **271** 个 |
| DEX | 31 个 |
| 签名 | v1+v2+v3，Xiaomi（MIUI）证书，**校验通过** |
| native | 仅 arm64-v8a |

## 二、组件面（按可达性）

对外可达 163 个，其中与「接替」直接相关的：

| 组件 | 作用 | 权限 |
|---|---|---|
| `com.aios.osbot.external.ExternalAgentService` | 外部 agent 接入 | `EXTERNAL_AGENT`（signature\|privileged） |
| `com.aios.osbot.external.CliCommandService` | 命令通道 | 同上 |
| `com.aios.osbot.external.ExternalAsrService` | 外部 ASR | 同上 |
| `com.aios.osbot.external.PendingIntentJumpService` | 跳转 | 同上 |
| `UIAgentServiceForOSBot` | 工具提供 | `BIND_APP_TOOL`（signature） |
| `LaunchHomeRouterActivity` | 快捷方式入口 | — |
| `com.xiaomi.voiceassistant.MainActivity`(alias) | 启动器入口 | — |
| `UploadStateService` | **exported 且无权限** | — |

> `UploadStateService` 是少见的一个「导出且无权限」的服务，值得单独看。

## 三、语音链路相关的 native 库

按功能分组（都是 arm64-v8a）：

| 能力 | 库 |
|---|---|
| 唤醒（KWS） | `libwakeup-engine.so`、`libwakeup-engine-1.1.3.so`、`libflexkws.so`、`libflexkws_jni.so` |
| VAD / 音量 | `libvad2.so`、`libvolume-detection.so` |
| 音频前端 | `libafe_sdk.so`（15.7 MB） |
| ASR 客户端 | `libaivs_jni.so`、`libaivsopus.so` |
| 编解码 | `libjlspeex.so`、`libmp3lame.so`、`libffavc.so` |
| 唤醒控制 | `libopen_voice_control_sdk.so` |
| 向量/推理 | `libMNN.so`、`libCVLM-DLA.so`、`libowl.so`、`libvector_engine_jni.so` |
| JS 运行时 | `libv8runtime.so`(18.9MB)、`libqjsruntime.so`、`libquickjs_android.so` |
| React Native | `libreact_*`、`libhermes*.so`、`libyoga.so` |
| 数据 | `libsqlcipher.so`、`libmmkv.so` |
| 稳定性 | `libxcrash*.so`、`libshadowhook.so`、`libbytehook.so` |

**结论**：她的语音栈是「AFE 音频前端 → VAD → KWS → ASR 客户端」四段式，
`libafe_sdk` 是前端核心，`libaivs_jni` 是识别客户端。

## 四、资源（assets 1007 个）

| 资源 | 用途 |
|---|---|
| `assets/wakeupModel/model_wayne_lavender.bin`（2.25 MB） | 唤醒模型 |
| `assets/flexkws/*.json5`（9 个） | 唤醒配置档（按平台/麦克风/位深） |
| `assets/flexkws/conf/ns_phone_model_*/final.tflite`（2×1.56 MB） | 降噪模型 |
| `assets/model/CVLM-DLA.mnn`（6.79 MB） | 视觉模型 |

## 五、权限面里值得注意的几条

| 权限 | 说明 |
|---|---|
| `ADD_TRUSTED_DISPLAY` | **可以建虚拟屏**（与我们方案冲突点也是机会点） |
| `MANAGE_VIRTUAL_DISPLAYS` | 同上 |
| `INJECT_EVENTS` | 可注入输入事件 |
| `CAPTURE_AUDIO_OUTPUT` | 可录音频输出 |
| `READ_FRAME_BUFFER` | 可读帧缓冲 |
| `FORCE_STOP_PACKAGES` | 可强停应用 |
| `START_ANY_ACTIVITY` | 可启动任意 Activity |

也就是说：**小爱本身就有我们想要的「后台操作手机」的能力**，只是被封装在她的技能里。

## 六、下一步

1. 查 `UploadStateService` 为什么导出且无权限（可能是可利用入口）
2. 沿 `libaivs_jni` 找 ASR 结果回调（R1 的核心）
3. 读 `com.xiaomi.ai.function.*` 摸技能路由（R2）
