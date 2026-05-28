<template>
  <aside class="sidebar" :class="{ collapsed: !visible }">
    <div class="sidebar-inner">
      <!-- Header: avatar + toggle -->
      <div class="sidebar-header">
        <div class="user-info" @click="handleAvatarClick">
          <div class="user-avatar">{{ (user?.nickname || user?.username || '?')[0] }}</div>
          <div v-if="visible" class="user-name">{{ user?.nickname || user?.username || '游客' }}</div>
        </div>
        <button class="icon-btn" @click="toggleSidebar" :title="visible ? '收起侧栏' : '展开侧栏'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path v-if="visible" d="M15 18l-6-6 6-6"/>
            <path v-else d="M9 18l6-6-6-6"/>
          </svg>
        </button>
      </div>

      <!-- User dropdown (expanded mode: inline menu; collapsed mode: popover) -->
      <transition name="menu-fade">
        <div v-if="showUserMenu && visible" class="user-menu">
          <button class="menu-item" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="menu-icon">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </button>
        </div>
      </transition>

      <!-- Collapsed mode: popover -->
      <transition name="menu-fade">
        <div v-if="showUserMenu && !visible" class="collapsed-popover">
          <div class="popover-user">
            <span class="popover-name">{{ user?.nickname || user?.username || '游客' }}</span>
          </div>
          <button class="menu-item" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="menu-icon">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </button>
        </div>
      </transition>

      <button class="new-chat-btn" @click="$emit('new-conversation')" :class="{ collapsed: !visible }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="plus-icon">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        <span v-if="visible">新建对话</span>
      </button>

      <div class="conversation-list" v-if="visible">
        <div v-if="conversations.length === 0" class="empty-hint">
          <div class="empty-emoji">💬</div>
          <p>还没有对话</p>
          <p class="empty-sub">点击上方按钮开始</p>
        </div>

        <button
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeId }"
          @click="$emit('select-conversation', conv)"
        >
          <div class="conv-main">
            <div class="conv-title">{{ conv.title || '新对话' }}</div>
            <div class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</div>
          </div>
          <div class="conv-actions">
            <span class="conv-time">{{ formatTime(conv.updatedAt || conv.createdAt) }}</span>
            <button class="conv-delete" @click.stop="$emit('delete-conversation', conv)" title="删除">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </button>
      </div>

      <!-- Collapsed mode: mini conversation avatars -->
      <div class="collapsed-list" v-if="!visible">
        <button
          v-for="conv in conversations"
          :key="conv.id"
          class="collapsed-item"
          :class="{ active: conv.id === activeId }"
          :title="conv.title"
          @click="$emit('select-conversation', conv)"
        >
          <span class="collapsed-dot"></span>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { apiLogout } from '../api/index.js'

const props = defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  visible: { type: Boolean, default: true }
})

const emit = defineEmits(['new-conversation', 'select-conversation', 'delete-conversation', 'toggle-sidebar'])
const router = useRouter()
const { user, clearAuth } = useAuth()
const showUserMenu = ref(false)

function toggleSidebar() {
  emit('toggle-sidebar')
  showUserMenu.value = false
}

function handleAvatarClick() {
  showUserMenu.value = !showUserMenu.value
}

async function handleLogout() {
  try { await apiLogout() } catch {}
  clearAuth()
  router.push('/login')
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
}

.sidebar.collapsed {
  width: 56px;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
  position: relative;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 4px;
  margin-bottom: 12px;
}

.sidebar.collapsed .sidebar-header {
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 10px;
  transition: background 0.2s;
  overflow: hidden;
}

.sidebar.collapsed .user-info {
  padding: 2px;
}

.user-info:hover {
  background: rgba(251, 191, 36, 0.08);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fbbf24, #f97316);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: none;
  background: rgba(251, 191, 36, 0.08);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.icon-btn:hover {
  background: rgba(251, 191, 36, 0.18);
  color: var(--sun-orange);
}

.icon-btn svg {
  width: 18px;
  height: 18px;
}

/* User menu (expanded) */
.user-menu {
  padding: 4px;
  margin-bottom: 8px;
}

/* Collapsed popover */
.collapsed-popover {
  position: absolute;
  left: 60px;
  top: 14px;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--card-border);
  border-radius: 14px;
  box-shadow: var(--shadow-lg);
  padding: 8px;
  min-width: 160px;
  z-index: 20;
}

.popover-user {
  padding: 8px 12px 6px;
  border-bottom: 1px solid rgba(251, 191, 36, 0.12);
  margin-bottom: 4px;
}

.popover-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.menu-item {
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

.menu-item:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.menu-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 1.5px dashed rgba(251, 191, 36, 0.4);
  border-radius: 14px;
  background: rgba(251, 191, 36, 0.04);
  color: var(--sun-orange);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 12px;
  white-space: nowrap;
}

.new-chat-btn:hover {
  background: rgba(251, 191, 36, 0.1);
  border-color: var(--sun-yellow);
  box-shadow: 0 2px 12px rgba(251, 191, 36, 0.12);
}

.new-chat-btn.collapsed {
  justify-content: center;
  padding: 10px;
}

.plus-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-hint {
  text-align: center;
  padding: 32px 16px;
}

.empty-emoji {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-hint p {
  font-size: 13px;
  color: var(--text-light);
}

.empty-sub {
  font-size: 11px !important;
  margin-top: 2px;
}

.conv-item {
  display: flex;
  flex-direction: column;
  padding: 12px 14px;
  border-radius: 14px;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
  gap: 2px;
}

.conv-item:hover {
  background: rgba(251, 191, 36, 0.06);
}

.conv-item.active {
  background: rgba(251, 191, 36, 0.12);
  border: 1px solid rgba(251, 191, 36, 0.25);
}

.conv-main {
  overflow: hidden;
}

.conv-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}

.conv-preview {
  font-size: 12px;
  color: var(--text-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.conv-time {
  font-size: 11px;
  color: var(--text-light);
}

.conv-delete {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: none;
  background: none;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.conv-item:hover .conv-delete {
  opacity: 1;
}

.conv-delete:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.conv-delete svg {
  width: 14px;
  height: 14px;
}

/* Collapsed conversation dots */
.collapsed-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
  overflow-y: auto;
}

.collapsed-item {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: rgba(148, 163, 184, 0.2);
  cursor: pointer;
  padding: 0;
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapsed-item:hover {
  background: rgba(251, 191, 36, 0.4);
  transform: scale(1.3);
}

.collapsed-item.active {
  background: var(--sun-yellow);
  box-shadow: 0 0 6px rgba(251, 191, 36, 0.5);
}
</style>
