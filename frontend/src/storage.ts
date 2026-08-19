// 统一管理 localStorage 的持久化钥匙，避免在多个 store 之间散落重复的字符串。
const TOKEN = 'testpilot.accessToken'
const USER_ID = 'testpilot.userId'
const PROJECT_ID = 'testpilot.projectId'

function readNumber(key: string): number | null {
  const raw = localStorage.getItem(key)
  if (raw == null || raw === '') return null
  const value = Number(raw)
  return Number.isFinite(value) ? value : null
}

export const storage = {
  get token(): string | null {
    return localStorage.getItem(TOKEN)
  },
  set token(value: string | null) {
    if (value) localStorage.setItem(TOKEN, value)
    else localStorage.removeItem(TOKEN)
  },
  get userId(): number | null {
    return readNumber(USER_ID)
  },
  set userId(value: number | null) {
    if (value == null) localStorage.removeItem(USER_ID)
    else localStorage.setItem(USER_ID, String(value))
  },
  get projectId(): number | null {
    return readNumber(PROJECT_ID)
  },
  set projectId(value: number | null) {
    if (value == null) localStorage.removeItem(PROJECT_ID)
    else localStorage.setItem(PROJECT_ID, String(value))
  }
}
