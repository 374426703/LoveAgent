<template>
  <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="visible" class="modal-overlay" @click.self="$emit('cancel')">
        <div class="modal-card">
          <div class="modal-emoji">{{ emoji }}</div>
          <h3 class="modal-title">{{ title }}</h3>
          <p class="modal-message">{{ message }}</p>
          <div class="modal-actions">
            <button v-if="showCancel" class="btn-cancel" @click="$emit('cancel')">{{ cancelText }}</button>
            <button class="btn-confirm" @click="$emit('confirm')">{{ confirmText }}</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '提示' },
  message: { type: String, default: '' },
  emoji: { type: String, default: '💡' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  showCancel: { type: Boolean, default: false }
})

defineEmits(['confirm', 'cancel'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.modal-card {
  background: var(--card-bg);
  backdrop-filter: blur(24px);
  border-radius: var(--radius-xl);
  border: 1px solid var(--card-border);
  box-shadow: var(--shadow-lg);
  padding: 36px 32px 28px;
  max-width: 380px;
  width: 100%;
  text-align: center;
}

.modal-emoji {
  font-size: 44px;
  margin-bottom: 12px;
  animation: floatEmoji 3s ease-in-out infinite;
}

@keyframes floatEmoji {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.modal-message {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 24px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.btn-cancel {
  padding: 10px 24px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(148, 163, 184, 0.06);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: rgba(148, 163, 184, 0.12);
}

.btn-confirm {
  padding: 10px 28px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: white;
  font-size: 14px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 3px 12px rgba(251, 191, 36, 0.3);
}

.btn-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 20px rgba(251, 191, 36, 0.4);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-card,
.modal-fade-leave-to .modal-card {
  transform: scale(0.92);
  opacity: 0;
}
</style>
