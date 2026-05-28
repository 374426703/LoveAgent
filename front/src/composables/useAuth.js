import { reactive, computed } from 'vue'

const state = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  initialized: false
})

export function useAuth() {
  const isLoggedIn = computed(() => !!state.token && !!state.user)
  const user = computed(() => state.user)

  function setAuth(token, userData) {
    state.token = token
    state.user = userData
    state.initialized = true
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function clearAuth() {
    state.token = ''
    state.user = null
    state.initialized = true
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function checkAuth() {
    const token = localStorage.getItem('token')
    if (!token) {
      clearAuth()
      return false
    }
    try {
      const resp = await fetch('/api/user/info', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const data = await resp.json()
      if (data.code === 0) {
        setAuth(token, data.data)
        return true
      } else {
        clearAuth()
        return false
      }
    } catch {
      clearAuth()
      return false
    }
  }

  function getAuthHeaders() {
    if (!state.token) return {}
    return { 'Authorization': `Bearer ${state.token}` }
  }

  return { state, isLoggedIn, user, setAuth, clearAuth, checkAuth, getAuthHeaders }
}
