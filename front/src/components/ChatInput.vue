<template>
  <div class="chat-input-area">
    <div class="input-container">
      <textarea
        ref="textareaRef"
        v-model="localMessage"
        class="message-input"
        :placeholder="placeholder"
        rows="1"
        :disabled="disabled"
        @keydown.enter.exact.prevent="handleSend"
        @keydown.enter.shift.exact="handleNewLine"
        @input="autoResize"
      ></textarea>
      <button
        class="send-btn"
        :class="{ active: canSend, disabled: !canSend || disabled }"
        :disabled="!canSend || disabled"
        @click="handleSend"
        :title="disabled ? '请等待回复完成' : '发送消息'"
      >
        <svg v-if="!disabled" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
        <svg v-else class="stop-icon" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2"/>
        </svg>
      </button>
    </div>
    <p class="input-hint">Enter 发送 · Shift+Enter 换行</p>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入你的消息...' }
})

const emit = defineEmits(['update:modelValue', 'send', 'stop'])

const textareaRef = ref(null)
const localMessage = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  localMessage.value = val
})

const canSend = computed(() => localMessage.value.trim().length > 0)

function autoResize() {
  nextTick(() => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 150) + 'px'
  })
}

function handleSend() {
  if (props.disabled) {
    emit('stop')
    return
  }
  if (!canSend.value) return
  emit('send', localMessage.value.trim())
  localMessage.value = ''
  emit('update:modelValue', '')
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  })
}

function handleNewLine() {
  const el = textareaRef.value
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  localMessage.value = localMessage.value.slice(0, start) + '\n' + localMessage.value.slice(end)
  nextTick(() => {
    el.selectionStart = el.selectionEnd = start + 1
    autoResize()
  })
}
</script>

<style scoped>
.chat-input-area {
  padding: 16px 20px 12px;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border-top: 1px solid var(--card-border);
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: rgba(251, 191, 36, 0.05);
  border: 1.5px solid rgba(251, 191, 36, 0.2);
  border-radius: 20px;
  padding: 8px 8px 8px 18px;
  transition: all 0.3s ease;
}

.input-container:focus-within {
  border-color: var(--sun-yellow);
  box-shadow: 0 0 0 3px rgba(251, 191, 36, 0.1);
  background: white;
}

.message-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--text-primary);
  resize: none;
  line-height: 1.55;
  padding: 6px 0;
  min-height: 24px;
  max-height: 150px;
}

.message-input::placeholder {
  color: var(--text-light);
}

.message-input:disabled {
  opacity: 0.7;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.12);
  color: var(--text-light);
}

.send-btn svg {
  width: 20px;
  height: 20px;
}

.send-btn.active {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: white;
  box-shadow: 0 3px 12px rgba(251, 191, 36, 0.35);
}

.send-btn.active:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.45);
}

.send-btn.disabled {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  box-shadow: 0 3px 12px rgba(239, 68, 68, 0.3);
  cursor: pointer;
}

.stop-icon {
  width: 16px !important;
  height: 16px !important;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--text-light);
  margin-top: 8px;
}
</style>
