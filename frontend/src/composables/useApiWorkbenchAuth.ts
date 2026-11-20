import { ref, watch } from 'vue'

interface AuthForm {
  headersJson: string
}

/**
 * 工作台认证信息：从请求头解析 Bearer，并在用户显式填写时写回 Authorization。
 */
export function useApiWorkbenchAuth(apiRequestForm: AuthForm) {
  const authMode = ref<'none' | 'bearer' | 'basic' | 'apikey'>('none')
  const authToken = ref('')
  const authUsername = ref('')
  const authPassword = ref('')
  const apiKeyName = ref('X-API-Key')
  const apiKeyValue = ref('')

  function parseAuthFromHeaders() {
    let headers: Record<string, unknown> = {}
    try {
      headers = JSON.parse(apiRequestForm.headersJson || '{}')
    } catch {
      return
    }
    const authorization = headers.Authorization ?? headers.authorization
    if (typeof authorization === 'string' && authorization.toLowerCase().startsWith('bearer ')) {
      authMode.value = 'bearer'
      authToken.value = authorization.slice(7).trim()
    }
  }

  function base64Utf8(value: string): string {
    const bytes = new TextEncoder().encode(value)
    let binary = ''
    bytes.forEach((byte) => {
      binary += String.fromCharCode(byte)
    })
    return btoa(binary)
  }

  function applyAuth() {
    let headers: Record<string, string> = {}
    try {
      headers = JSON.parse(apiRequestForm.headersJson || '{}')
    } catch {
      headers = {}
    }
    // 只在用户显式填写时覆盖请求头，避免把已保存的 Authorization 静默删掉。
    if (authMode.value === 'bearer' && authToken.value.trim()) {
      headers.Authorization = `Bearer ${authToken.value.trim()}`
    } else if (authMode.value === 'basic' && (authUsername.value || authPassword.value)) {
      headers.Authorization = `Basic ${base64Utf8(`${authUsername.value}:${authPassword.value}`)}`
    } else if (authMode.value === 'apikey' && apiKeyName.value.trim() && apiKeyValue.value.trim()) {
      headers[apiKeyName.value.trim()] = apiKeyValue.value.trim()
    }
    apiRequestForm.headersJson = JSON.stringify(headers, null, 2)
  }

  watch(() => apiRequestForm.headersJson, parseAuthFromHeaders)

  return {
    authMode,
    authToken,
    authUsername,
    authPassword,
    apiKeyName,
    apiKeyValue,
    applyAuth
  }
}
