const BASE = ''

function getToken() {
  return localStorage.getItem('token') || ''
}

function authHeaders() {
  const token = getToken()
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

async function request(url, options = {}) {
  const resp = await fetch(BASE + url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...options.headers
    }
  })
  if (resp.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }
  return resp.json()
}

// ========== Auth APIs ==========

export function apiLogin(username, password) {
  return request('/api/user/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
}

export function apiRegister(username, password, nickname) {
  return request('/api/user/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, nickname })
  })
}

export function apiGetUserInfo() {
  return request('/api/user/info')
}

export function apiLogout() {
  return request('/api/user/logout', { method: 'POST' })
}

// ========== Conversation APIs ==========

export function apiListConversations(appType = 'love_app') {
  return request(`/api/conversations?appType=${appType}`)
}

export function apiCreateConversation(appType, title = '新对话') {
  return request('/api/conversations', {
    method: 'POST',
    body: JSON.stringify({ appType, title })
  })
}

export function apiGetConversationMessages(conversationId) {
  return request(`/api/conversations/${conversationId}/messages`)
}

export function apiDeleteConversation(conversationId) {
  return request(`/api/conversations/${conversationId}`, { method: 'DELETE' })
}

export function apiUpdateConversationTitle(conversationId, title) {
  return request(`/api/conversations/${conversationId}/title`, {
    method: 'PUT',
    body: JSON.stringify({ title })
  })
}

// ========== Chat SSE ==========

export function chatWithLoveApp(message, chatId, onMessage, onDone, onError) {
  const params = new URLSearchParams({ message, chatId })
  const url = `/ai/love_app/chat/sse?${params}`
  return createSSEConnection(url, onMessage, onDone, onError)
}

export function chatWithSuperAgent(message, chatId, onMessage, onDone, onError) {
  const params = new URLSearchParams({ message, chatId })
  const url = `/ai/manus/chat?${params}`
  return createSSEConnection(url, onMessage, onDone, onError)
}

function createSSEConnection(url, onMessage, onDone, onError) {
  let aborted = false
  let reader = null
  const controller = new AbortController()

  fetch(url, {
    signal: controller.signal,
    headers: { 'Accept': 'text/event-stream', ...authHeaders() }
  })
    .then(response => {
      if (response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function processChunk({ done, value }) {
        if (aborted) return
        if (done) { if (onDone) onDone(); return }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data && data !== '[DONE]') {
              try {
                const parsed = JSON.parse(data)
                const text = typeof parsed === 'string' ? parsed : (parsed.content || parsed.text || JSON.stringify(parsed))
                if (text) onMessage(text)
              } catch {
                onMessage(data)
              }
            }
          } else if (line.trim() && !line.startsWith(':')) {
            onMessage(line.trim())
          }
        }
        return reader.read().then(processChunk)
      }
      return reader.read().then(processChunk)
    })
    .catch(err => { if (!aborted && onError) onError(err) })

  return {
    abort() {
      aborted = true
      controller.abort()
      if (reader) reader.cancel().catch(() => {})
    }
  }
}
