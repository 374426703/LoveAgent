<template>
  <div class="login-page">
    <!-- Animated background orbs -->
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="orb orb-4"></div>
      <div class="orb orb-5"></div>
    </div>

    <!-- Floating particles -->
    <div class="particles">
      <div v-for="n in 20" :key="n" class="particle" :style="particleStyle(n)"></div>
    </div>

    <!-- Grid pattern overlay -->
    <div class="grid-overlay"></div>

    <!-- Main content -->
    <div class="login-wrapper">
      <!-- Left: Branding panel -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <div class="icon-ring"></div>
            <span class="icon-emoji">☀️</span>
          </div>
          <h1 class="brand-title">Love AI Agent</h1>
          <p class="brand-subtitle">新一代智能助手平台</p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>AI 驱动的智能对话</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>多场景应用支持</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>安全可靠的数据保护</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Form panel -->
      <div class="form-panel">
        <div class="form-card">
          <!-- Card header -->
          <div class="form-header">
            <div class="form-logo">☀️</div>
            <h2 class="form-title">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
            <p class="form-desc">{{ mode === 'login' ? '登录你的 Love AI Agent 账号' : '注册一个新账号开始使用' }}</p>
          </div>

          <!-- Tab switch -->
          <div class="tab-bar">
            <button
              :class="['tab-btn', { active: mode === 'login' }]"
              @click="switchMode('login')"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="tab-icon">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                <polyline points="10 17 15 12 10 7"/>
                <line x1="15" y1="12" x2="3" y2="12"/>
              </svg>
              登录
            </button>
            <button
              :class="['tab-btn', { active: mode === 'register' }]"
              @click="switchMode('register')"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="tab-icon">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="8.5" cy="7" r="4"/>
                <line x1="20" y1="8" x2="20" y2="14"/>
                <line x1="23" y1="11" x2="17" y2="11"/>
              </svg>
              注册
            </button>
            <div class="tab-indicator" :class="{ right: mode === 'register' }"></div>
          </div>

          <!-- Form -->
          <form class="auth-form" @submit.prevent="handleSubmit">
            <TransitionGroup name="field" tag="div" class="fields-wrapper">
              <!-- Username -->
              <div class="input-group" key="username">
                <label class="input-label">用户名</label>
                <div class="input-box" :class="{ focused: focusUser, error: errorMsg }">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="input-icon">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  <input
                    v-model="username"
                    type="text"
                    placeholder="请输入用户名"
                    autocomplete="username"
                    required
                    @focus="focusUser = true"
                    @blur="focusUser = false"
                  />
                </div>
              </div>

              <!-- Nickname (register only) -->
              <div v-if="mode === 'register'" class="input-group" key="nickname">
                <label class="input-label">昵称 <span class="label-optional">选填</span></label>
                <div class="input-box" :class="{ focused: focusNick }">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="input-icon">
                    <path d="M12 20h9"/>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                  </svg>
                  <input
                    v-model="nickname"
                    type="text"
                    placeholder="给自己取个名字吧"
                    autocomplete="nickname"
                    @focus="focusNick = true"
                    @blur="focusNick = false"
                  />
                </div>
              </div>

              <!-- Password -->
              <div class="input-group" key="password">
                <label class="input-label">密码</label>
                <div class="input-box" :class="{ focused: focusPass, error: errorMsg }">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="input-icon">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                  </svg>
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    autocomplete="current-password"
                    required
                    @focus="focusPass = true"
                    @blur="focusPass = false"
                  />
                  <button type="button" class="eye-btn" @click="showPassword = !showPassword" tabindex="-1">
                    <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  </button>
                </div>
              </div>
            </TransitionGroup>

            <!-- Error message -->
            <Transition name="error-slide">
              <div v-if="errorMsg" class="error-msg">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="error-icon">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="15" y1="9" x2="9" y2="15"/>
                  <line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
                {{ errorMsg }}
              </div>
            </Transition>

            <!-- Submit button -->
            <button type="submit" class="submit-btn" :disabled="loading" :class="{ loading }">
              <span v-if="!loading" class="btn-text">
                {{ mode === 'login' ? '登 录' : '注 册' }}
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="btn-arrow">
                  <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </span>
              <span v-else class="btn-loading">
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
              </span>
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { apiLogin, apiRegister } from '../api/index.js'

const router = useRouter()
const { setAuth } = useAuth()

const mode = ref('login')
const username = ref('')
const password = ref('')
const nickname = ref('')
const errorMsg = ref('')
const loading = ref(false)
const showPassword = ref(false)
const focusUser = ref(false)
const focusPass = ref(false)
const focusNick = ref(false)

function switchMode(m) {
  mode.value = m
  errorMsg.value = ''
}

function particleStyle(n) {
  const size = 2 + (n % 4)
  const left = (n * 17 + 5) % 100
  const top = (n * 23 + 10) % 100
  const delay = (n * 0.7) % 8
  const duration = 12 + (n % 10)
  const opacity = 0.15 + (n % 5) * 0.08
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    opacity
  }
}

async function handleSubmit() {
  errorMsg.value = ''
  if (!username.value.trim() || !password.value.trim()) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  loading.value = true

  try {
    let result
    if (mode.value === 'login') {
      result = await apiLogin(username.value.trim(), password.value)
    } else {
      result = await apiRegister(username.value.trim(), password.value, nickname.value.trim() || username.value.trim())
    }

    if (result.code === 0) {
      setAuth(result.data.token, result.data.user)
      router.push('/')
    } else {
      errorMsg.value = result.message || '操作失败'
    }
  } catch {
    errorMsg.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== Page ===== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

/* ===== Background Orbs ===== */
.bg-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.orb-1 {
  width: 500px;
  height: 500px;
  top: -15%;
  right: -10%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.4), rgba(251, 146, 60, 0.15));
  animation: orbFloat1 18s ease-in-out infinite;
}

.orb-2 {
  width: 350px;
  height: 350px;
  bottom: -10%;
  left: -8%;
  background: radial-gradient(circle, rgba(251, 146, 60, 0.35), rgba(239, 68, 68, 0.1));
  animation: orbFloat2 22s ease-in-out infinite;
}

.orb-3 {
  width: 250px;
  height: 250px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(251, 113, 133, 0.2), transparent);
  animation: orbFloat3 15s ease-in-out infinite;
}

.orb-4 {
  width: 180px;
  height: 180px;
  top: 20%;
  left: 15%;
  background: radial-gradient(circle, rgba(253, 230, 138, 0.35), transparent);
  animation: orbFloat2 20s ease-in-out infinite reverse;
}

.orb-5 {
  width: 220px;
  height: 220px;
  bottom: 20%;
  right: 20%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.2), transparent);
  animation: orbFloat1 25s ease-in-out infinite reverse;
}

@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, 40px) scale(1.05); }
  66% { transform: translate(25px, -15px) scale(0.9); }
}

@keyframes orbFloat3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.3); }
}

/* ===== Particles ===== */
.particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  background: rgba(251, 191, 36, 0.6);
  border-radius: 50%;
  animation: particleDrift linear infinite;
}

@keyframes particleDrift {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-120px) translateX(40px); opacity: 0; }
}

/* ===== Grid Overlay ===== */
.grid-overlay {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(251, 191, 36, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(251, 191, 36, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 1;
}

/* ===== Main Wrapper ===== */
.login-wrapper {
  position: relative;
  z-index: 10;
  display: flex;
  width: 100%;
  max-width: 960px;
  min-height: 580px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset,
    0 1px 0 rgba(255, 255, 255, 0.3) inset;
}

/* ===== Brand Panel ===== */
.brand-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 40px;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.15) 0%, rgba(251, 146, 60, 0.08) 50%, rgba(239, 68, 68, 0.05) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
  position: relative;
  overflow: hidden;
}

.brand-panel::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 50%, rgba(251, 191, 36, 0.12) 0%, transparent 50%);
  animation: brandGlow 10s ease-in-out infinite;
  pointer-events: none;
}

@keyframes brandGlow {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10%, -5%); }
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
}

.icon-ring {
  position: absolute;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 2px solid rgba(251, 191, 36, 0.3);
  animation: ringPulse 3s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.15); opacity: 0.2; }
}

.icon-emoji {
  font-size: 40px;
  filter: drop-shadow(0 4px 12px rgba(251, 191, 36, 0.3));
  animation: iconFloat 4s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.brand-title {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 50%, #ef4444 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 10px;
}

.brand-subtitle {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
  letter-spacing: 0.02em;
  margin-bottom: 40px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fbbf24, #f97316);
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(251, 191, 36, 0.4);
}

.brand-footer {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: var(--text-light);
  font-weight: 500;
  letter-spacing: 0.05em;
}

/* ===== Form Panel ===== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  min-width: 0;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
}

.form-logo {
  font-size: 32px;
  margin-bottom: 16px;
  display: inline-block;
  animation: iconFloat 4s ease-in-out infinite;
}

.form-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 6px;
  letter-spacing: -0.01em;
}

.form-desc {
  font-size: 14px;
  color: var(--text-light);
  font-weight: 400;
}

/* ===== Tab Bar ===== */
.tab-bar {
  position: relative;
  display: flex;
  background: rgba(251, 191, 36, 0.06);
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 28px;
  border: 1px solid rgba(251, 191, 36, 0.1);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  background: none;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: var(--text-light);
  cursor: pointer;
  transition: color 0.3s ease;
  position: relative;
  z-index: 1;
}

.tab-btn.active {
  color: var(--sun-orange);
}

.tab-icon {
  width: 16px;
  height: 16px;
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: white;
  border-radius: 11px;
  box-shadow: 0 2px 10px rgba(251, 191, 36, 0.15);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-indicator.right {
  transform: translateX(100%);
}

/* ===== Form Fields ===== */
.fields-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  padding-left: 2px;
}

.label-optional {
  font-weight: 400;
  color: var(--text-light);
  font-size: 12px;
}

.input-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 1.5px solid rgba(251, 191, 36, 0.12);
  border-radius: 14px;
  transition: all 0.3s ease;
  height: 50px;
}

.input-box.focused {
  border-color: var(--sun-yellow);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 4px rgba(251, 191, 36, 0.08), 0 2px 12px rgba(251, 191, 36, 0.08);
}

.input-box.error {
  border-color: rgba(239, 68, 68, 0.5);
  animation: shake 0.4s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.input-icon {
  width: 20px;
  height: 20px;
  color: var(--text-light);
  flex-shrink: 0;
  transition: color 0.3s ease;
}

.input-box.focused .input-icon {
  color: var(--sun-orange);
}

.input-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--text-primary);
  min-width: 0;
}

.input-box input::placeholder {
  color: var(--text-light);
  font-weight: 400;
}

.eye-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--text-light);
  border-radius: 8px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.eye-btn:hover {
  color: var(--sun-orange);
  background: rgba(251, 191, 36, 0.08);
}

.eye-btn svg {
  width: 18px;
  height: 18px;
}

/* ===== Error Message ===== */
.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 12px;
  color: #dc2626;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.error-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.error-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.error-slide-leave-active {
  transition: all 0.2s ease;
}

.error-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== Submit Button ===== */
.submit-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  color: white;
  font-size: 16px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.06em;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.35);
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(245, 158, 11, 0.45);
}

.submit-btn:hover:not(:disabled)::before {
  opacity: 1;
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(245, 158, 11, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-arrow {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.submit-btn:hover:not(:disabled) .btn-arrow {
  transform: translateX(4px);
}

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  animation: dotBounce 1.4s ease-in-out infinite;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== Field Transitions ===== */
.field-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.field-leave-active {
  transition: all 0.25s ease;
}

.field-enter-from {
  opacity: 0;
  transform: translateY(-12px) scale(0.98);
}

.field-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    max-width: 440px;
    min-height: auto;
  }

  .brand-panel {
    padding: 36px 28px 28px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-subtitle {
    margin-bottom: 20px;
  }

  .brand-features {
    gap: 10px;
  }

  .brand-footer {
    display: none;
  }

  .form-panel {
    padding: 28px 24px 36px;
  }

  .form-title {
    font-size: 22px;
  }
}

@media (max-width: 420px) {
  .login-page {
    padding: 12px;
  }

  .login-wrapper {
    border-radius: 22px;
  }

  .brand-panel {
    padding: 28px 20px 20px;
  }

  .form-panel {
    padding: 20px 18px 28px;
  }

  .input-box {
    height: 46px;
  }

  .submit-btn {
    height: 46px;
  }
}
</style>
