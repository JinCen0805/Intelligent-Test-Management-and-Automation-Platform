import { describe, expect, it } from 'vitest'
import { buildCurl, normalizeJson, parseCurl, pretty } from './apiWorkbench'

describe('apiWorkbench utils', () => {
  it('pretty formats valid JSON and falls back on invalid input', () => {
    expect(pretty('{"a":1}', '{}')).toBe('{\n  "a": 1\n}')
    expect(pretty('not-json', '[]')).toBe('[]')
  })

  it('normalizeJson shares pretty fallback behaviour', () => {
    expect(normalizeJson('{"b":2}', '{}')).toContain('"b": 2')
    expect(normalizeJson('{bad', '{}')).toBe('{}')
  })

  it('buildCurl includes method, url, headers and body', () => {
    const curl = buildCurl({
      method: 'post',
      url: 'https://api.example.com/items',
      headersJson: '{"Content-Type":"application/json"}',
      body: '{"name":"x"}'
    })
    expect(curl).toContain('--request POST')
    expect(curl).toContain('https://api.example.com/items')
    expect(curl).toContain('Content-Type: application/json')
    expect(curl).toContain('--data-raw')
  })

  it('parseCurl extracts url, method, headers and body', () => {
    const parsed = parseCurl(
      "curl --request POST 'https://api.example.com/items' -H 'Content-Type: application/json' --data-raw '{\"a\":1}'"
    )
    expect(parsed).not.toBeNull()
    expect(parsed?.method).toBe('POST')
    expect(parsed?.url).toBe('https://api.example.com/items')
    expect(parsed?.headers['Content-Type']).toBe('application/json')
    expect(parsed?.body).toBe('{"a":1}')
  })

  it('parseCurl defaults to GET and rejects input without URL', () => {
    expect(parseCurl('curl https://api.example.com/ping')?.method).toBe('GET')
    expect(parseCurl('curl example.com')).toBeNull()
  })
})
