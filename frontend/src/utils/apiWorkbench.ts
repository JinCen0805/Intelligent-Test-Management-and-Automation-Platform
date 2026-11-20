/** 接口工作台的纯文本/命令转换工具（便于单测与复用）。 */

export function pretty(value: string, fallback: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return fallback
  }
}

export function normalizeJson(value: string, fallback: string): string {
  return pretty(value, fallback)
}

interface CurlSource {
  method: string
  url: string
  headersJson: string
  body: string
}

export function buildCurl(source: CurlSource): string {
  const method = source.method.toUpperCase()
  let headers: Record<string, string> = {}
  try {
    headers = JSON.parse(source.headersJson || '{}')
  } catch {
    /* 服务端保存时会再校验 */
  }
  const headerText = Object.entries(headers)
    .map(([key, value]) => ` -H ${JSON.stringify(`${key}: ${value}`)}`)
    .join('')
  const body = source.body.trim() ? ` --data-raw ${JSON.stringify(source.body)}` : ''
  return `curl --request ${method} ${JSON.stringify(source.url.trim() || 'https://example.com')}${headerText}${body}`
}

interface ParsedCurl {
  method: string
  url: string
  headers: Record<string, string>
  body: string
}

export function parseCurl(value: string): ParsedCurl | null {
  const text = value.trim()
  const url = text.match(/(?:curl\s+.*?)(?:--url\s+|-X\s+\w+\s+)?(['"]?https?:\/\/[^'"\s]+['"]?)/i)?.[1]
  if (!url) {
    return null
  }
  const method = text.match(/(?:-X|--request)\s+([A-Z]+)/i)?.[1]?.toUpperCase() || (text.includes('--data') ? 'POST' : 'GET')
  const headers: Record<string, string> = {}
  for (const match of text.matchAll(/(?:-H|--header)\s+['"]([^'"]+)['"]/gi)) {
    const separator = match[1].indexOf(':')
    if (separator > 0) headers[match[1].slice(0, separator).trim()] = match[1].slice(separator + 1).trim()
  }
  const body = text.match(/(?:--data-raw|--data|--data-binary|-d)\s+(['"])([\s\S]*?)\1/i)?.[2] || ''
  return {
    method,
    url: url.replace(/^['"]|['"]$/g, ''),
    headers,
    body
  }
}
