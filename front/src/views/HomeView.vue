<template>
  <div class="home">
    <div class="sun-decoration"></div>
    <div class="clouds">
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
    </div>

    <!-- Top user bar -->
    <nav class="top-bar">
      <div class="top-bar-inner">
        <div class="brand">
          <span class="brand-emoji">☀️</span>
          <span class="brand-name">Love AI Agent</span>
        </div>
        <div class="user-area">
          <div class="user-info" @click="showDropdown = !showDropdown">
            <div class="user-avatar">{{ (user?.nickname || user?.username || '?')[0] }}</div>
            <span class="user-name">{{ user?.nickname || user?.username || '游客' }}</span>
            <svg class="dropdown-arrow" :class="{ open: showDropdown }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </div>
          <transition name="dropdown-fade">
            <div v-if="showDropdown" class="dropdown-menu">
              <div class="dropdown-header">
                <span>{{ user?.nickname || user?.username }}</span>
                <span v-if="user?.role === 'ADMIN'" class="admin-badge">管理员</span>
              </div>
              <router-link v-if="user?.role === 'ADMIN'" to="/admin" class="dropdown-item admin-link" @click="showDropdown = false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
                管理控制台
              </router-link>
              <button class="dropdown-item logout" @click="handleLogout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                  <polyline points="16 17 21 12 16 7"/>
                  <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
                退出登录
              </button>
            </div>
          </transition>
        </div>
      </div>
    </nav>

    <header class="hero">
      <div class="hero-badge">✨ AI 驱动</div>
      <h1 class="hero-title">
        <span class="title-line">Love AI Agent</span>
        <span class="subtitle-line">你的专属智能助手</span>
      </h1>
      <p class="hero-desc">选择你想要的 AI 助手，开启一段奇妙旅程</p>
    </header>

    <main class="cards-section">
      <div class="cards-container">
        <router-link to="/love-master" class="app-card love-card">
          <div class="card-glow"></div>
          <div class="card-emoji">💕</div>
          <div class="card-icon-wrap">
            <div class="card-icon love-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </div>
          </div>
          <h2 class="card-title">恋爱大师</h2>
          <p class="card-desc">专业的情感顾问，为你的恋爱难题提供温暖建议</p>
          <div class="card-tags">
            <span class="tag">情感分析</span>
            <span class="tag">约会建议</span>
            <span class="tag">关系修复</span>
          </div>
          <div class="card-action">
            <span>开始咨询</span>
            <svg class="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </div>
        </router-link>

        <router-link to="/super-agent" class="app-card super-card">
          <div class="card-glow"></div>
          <div class="card-emoji">🦸</div>
          <div class="card-icon-wrap">
            <div class="card-icon super-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
            </div>
          </div>
          <h2 class="card-title">超级特工</h2>
          <p class="card-desc">全能型 AI 助手，处理复杂任务、搜索信息、生成内容</p>
          <div class="card-tags">
            <span class="tag">联网搜索</span>
            <span class="tag">文件操作</span>
            <span class="tag">PDF 生成</span>
          </div>
          <div class="card-action">
            <span>开始任务</span>
            <svg class="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </div>
        </router-link>
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { apiLogout } from '../api/index.js'

const router = useRouter()
const { user, clearAuth } = useAuth()
const showDropdown = ref(false)

async function handleLogout() {
  try { await apiLogout() } catch {}
  clearAuth()
  router.push('/login')
}

// Close dropdown when clicking outside
if (typeof window !== 'undefined') {
  window.addEventListener('click', (e) => {
    if (!e.target.closest('.user-area')) {
      showDropdown.value = false
    }
  })
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  padding: 32px 24px 48px;
}

/* Sun decoration */
.sun-decoration {
  position: fixed;
  top: -120px;
  right: -120px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.25) 0%, rgba(251, 146, 60, 0.08) 50%, transparent 70%);
  pointer-events: none;
  animation: sunPulse 6s ease-in-out infinite;
}

@keyframes sunPulse {
  0%, 100% { transform: scale(1); opacity: 0.7; }
  50% { transform: scale(1.15); opacity: 1; }
}

/* Clouds */
.clouds {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.cloud-1 {
  width: 120px;
  height: 40px;
  top: 8%;
  left: 10%;
  animation: cloudFloat 20s linear infinite;
}

.cloud-2 {
  width: 160px;
  height: 50px;
  top: 15%;
  right: 12%;
  animation: cloudFloat 25s linear infinite reverse;
}

.cloud-3 {
  width: 100px;
  height: 35px;
  top: 22%;
  left: 55%;
  animation: cloudFloat 18s linear infinite;
}

@keyframes cloudFloat {
  0% { transform: translateX(0); }
  100% { transform: translateX(80px); }
}

/* Top bar */
.top-bar {
  position: relative;
  z-index: 10;
  width: 100%;
}

.top-bar-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-emoji {
  font-size: 24px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-area {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 6px;
  border-radius: 14px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-info:hover {
  border-color: var(--sun-yellow);
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.12);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fbbf24, #f97316);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.user-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-arrow {
  width: 16px;
  height: 16px;
  color: var(--text-light);
  transition: transform 0.2s ease;
}

.dropdown-arrow.open {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--card-border);
  border-radius: 14px;
  box-shadow: var(--shadow-lg);
  min-width: 170px;
  padding: 6px;
  z-index: 20;
}

.dropdown-header {
  padding: 8px 12px 6px;
  border-bottom: 1px solid rgba(251, 191, 36, 0.12);
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-align: center;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 10px;
  background: none;
  font-size: 13.5px;
  font-family: inherit;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.dropdown-item svg {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.dropdown-item.logout:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.admin-link {
  color: var(--sun-orange);
}
.admin-link:hover {
  background: rgba(251, 191, 36, 0.08);
}

.admin-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 100px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: white;
  margin-left: 6px;
  vertical-align: middle;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: all 0.2s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Hero */
.hero {
  text-align: center;
  margin-top: 20px;
  margin-bottom: 48px;
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-block;
  padding: 6px 20px;
  border-radius: 100px;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.2), rgba(251, 146, 60, 0.15));
  border: 1px solid rgba(251, 191, 36, 0.3);
  font-size: 13px;
  font-weight: 600;
  color: var(--sun-orange);
  margin-bottom: 24px;
  letter-spacing: 0.02em;
}

.hero-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title-line {
  font-size: 48px;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 40%, #ef4444 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.subtitle-line {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
}

.hero-desc {
  margin-top: 20px;
  font-size: 16px;
  color: var(--text-secondary);
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

/* Cards */
.cards-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 900px;
  position: relative;
  z-index: 1;
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
  width: 100%;
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 32px 32px;
  border-radius: var(--radius-xl);
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--card-border);
  box-shadow: var(--shadow-md);
  text-decoration: none;
  color: inherit;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg), var(--shadow-glow);
}

.love-card:hover {
  border-color: rgba(251, 113, 133, 0.5);
}

.super-card:hover {
  border-color: rgba(251, 191, 36, 0.5);
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.love-card .card-glow {
  background: radial-gradient(ellipse at center, rgba(251, 113, 133, 0.08) 0%, transparent 60%);
}

.super-card .card-glow {
  background: radial-gradient(ellipse at center, rgba(251, 191, 36, 0.08) 0%, transparent 60%);
}

.app-card:hover .card-glow {
  opacity: 1;
}

.card-emoji {
  font-size: 48px;
  margin-bottom: 16px;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-card:hover .card-emoji {
  transform: scale(1.15) rotate(-5deg);
}

.card-icon-wrap {
  margin-bottom: 20px;
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s ease;
}

.card-icon svg {
  width: 28px;
  height: 28px;
}

.love-icon {
  background: linear-gradient(135deg, #fce7f3, #fbcfe8);
  color: #ec4899;
  box-shadow: 0 4px 12px rgba(236, 72, 153, 0.2);
}

.super-icon {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #f59e0b;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}

.app-card:hover .card-icon {
  transform: scale(1.08);
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: -0.01em;
}

.card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 20px;
  max-width: 280px;
}

.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 24px;
}

.tag {
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(251, 191, 36, 0.1);
  color: var(--sun-orange);
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.love-card .tag {
  background: rgba(251, 113, 133, 0.08);
  color: #e11d48;
  border: 1px solid rgba(251, 113, 133, 0.2);
}

.card-action {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--sun-orange);
  transition: gap 0.3s ease;
}

.love-card .card-action {
  color: #e11d48;
}

.app-card:hover .card-action {
  gap: 14px;
}

.arrow {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.app-card:hover .arrow {
  transform: translateX(3px);
}

/* Footer */
.home-footer {
  margin-top: 64px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.home-footer p {
  font-size: 13px;
  color: var(--text-light);
}

/* Responsive */
@media (max-width: 700px) {
  .cards-container {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .title-line {
    font-size: 34px;
  }

  .subtitle-line {
    font-size: 18px;
  }

  .hero {
    margin-top: 24px;
    margin-bottom: 40px;
  }

  .app-card {
    padding: 32px 24px 28px;
  }
}
</style>
