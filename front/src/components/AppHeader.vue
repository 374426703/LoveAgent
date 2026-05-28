<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="back-btn" :title="backTitle">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
      </router-link>
      <div class="header-info">
        <span class="header-emoji">{{ emoji }}</span>
        <h1 class="header-title">{{ title }}</h1>
      </div>
    </div>
    <div class="header-right">
      <div class="status-badge" :class="{ connected: isConnected }">
        <span class="status-dot"></span>
        <span>{{ isConnected ? '已连接' : '等待连接' }}</span>
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  emoji: { type: String, required: true },
  backTitle: { type: String, default: '返回首页' },
  isConnected: { type: Boolean, default: false }
})
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--card-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(251, 191, 36, 0.1);
  color: var(--sun-orange);
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.back-btn:hover {
  background: rgba(251, 191, 36, 0.2);
  transform: translateX(-2px);
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-emoji {
  font-size: 24px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.header-right {
  flex-shrink: 0;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-light);
  background: rgba(148, 163, 184, 0.1);
  transition: all 0.3s ease;
}

.status-badge.connected {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.1);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@media (max-width: 500px) {
  .app-header {
    padding: 12px 16px;
  }
  .header-title {
    font-size: 16px;
  }
}
</style>
