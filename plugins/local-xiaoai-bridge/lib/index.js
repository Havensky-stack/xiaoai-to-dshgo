// 小爱桥：显示/切换小爱静音，展示助理角色、模块加载状态与最近转发文本。
// 数据来源：/sdcard/Download/DSH-Go/ 下的两个文件（由 LSPosed 模块与后续版本写入）。
// 文件缺失时按「尚未上报」显示，不编造数据。
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { dirname } from 'node:path'

export const name = 'local-xiaoai-bridge'

const DIR = '/sdcard/Download/DSH-Go'
const MUTE_FILE = `${DIR}/xiaoai-mute.txt`
const STATUS_FILE = `${DIR}/xiaoai-bridge-status.json`
const RECENT_MS = 60_000

function isMuted() {
  try {
    return readFileSync(MUTE_FILE, 'utf8').trim() === '1'
  } catch {
    return false
  }
}

function setMuted(v) {
  try {
    mkdirSync(dirname(MUTE_FILE), { recursive: true })
    writeFileSync(MUTE_FILE, v ? '1' : '0')
    return true
  } catch {
    return false
  }
}

function readStatus() {
  try {
    const raw = JSON.parse(readFileSync(STATUS_FILE, 'utf8'))
    return {
      role: typeof raw.role === 'string' ? raw.role : null,
      moduleLoaded: raw.moduleLoaded === true ? true : raw.moduleLoaded === false ? false : null,
      lastText: typeof raw.lastText === 'string' ? raw.lastText : null,
      lastAt: typeof raw.lastAt === 'number' ? raw.lastAt : null,
    }
  } catch {
    return { role: null, moduleLoaded: null, lastText: null, lastAt: null }
  }
}

function stripText(zh) {
  const muted = isMuted()
  const st = readStatus()
  const recent = st.lastAt !== null && Date.now() - st.lastAt < RECENT_MS
  if (muted) return zh ? '小爱桥：小爱静音' : 'Bridge: muted'
  if (recent) return zh ? '小爱桥：转发中' : 'Bridge: relaying'
  return zh ? '小爱桥：待机' : 'Bridge: idle'
}

export function apply(ctx) {
  ctx.inject(['dshGo'], (ctx) => {
    // 胶囊点按：切换静音
    ctx.dshGo.on('state', (scope, event) => {
      const zh = !String(event.locale).startsWith('en')
      const next = !isMuted()
      if (!setMuted(next)) {
        return { toast: zh ? '写入静音开关失败' : 'Could not save the mute flag' }
      }
      return {
        toast: next
          ? zh ? '已静音小爱播报' : 'Xiaoai speech muted'
          : zh ? '已恢复小爱播报' : 'Xiaoai speech restored',
        refresh: true,
      }
    })

    // 状态条：三种状态
    ctx.dshGo.strip('state', (scope, emit, caps) => {
      const zh = !String(caps.locale).startsWith('en')
      const tick = () => emit({ text: stripText(zh), icon: 'robot' })
      tick()
      const timer = setInterval(tick, 5_000)
      return () => clearInterval(timer)
    })

    // 面板：角色 / 模块 / 最近转发
    ctx.dshGo.panel('status', {
      view: (scope, caps) => {
        const zh = !String(caps.locale).startsWith('en')
        return {
          type: 'stack',
          title: zh ? '小爱桥' : 'Xiaoai Bridge',
          children: [
            {
              type: 'keyValue',
              items: [
                [zh ? '助理角色' : 'Assistant role', { $: 'data.role' }],
                [zh ? '模块加载' : 'Module loaded', { $: 'data.module' }],
                [zh ? '最近转发' : 'Last relay', { $: 'data.last' }],
                [zh ? '静音状态' : 'Muted', { $: 'data.muted' }],
              ],
            },
            { type: 'actions', items: [{ label: zh ? '刷新' : 'Refresh', action: 'refresh' }] },
          ],
        }
      },
      data: (scope, emit, caps) => {
        const zh = !String(caps.locale).startsWith('en')
        const push = () => {
          const st = readStatus()
          emit({
            role: st.role ?? (zh ? '尚未上报' : 'not reported'),
            module:
              st.moduleLoaded === null
                ? zh ? '尚未上报' : 'not reported'
                : st.moduleLoaded
                  ? zh ? '已加载' : 'loaded'
                  : zh ? '未加载' : 'not loaded',
            last: st.lastText ?? (zh ? '尚无记录' : 'nothing yet'),
            muted: isMuted() ? (zh ? '开' : 'on') : zh ? '关' : 'off',
          })
        }
        push()
        const timer = setInterval(push, 10_000)
        return () => clearInterval(timer)
      },
      actions: {
        refresh: () => ({ refresh: true }),
      },
    })
  })
}
