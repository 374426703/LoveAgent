<template>
  <div class="typing-wrapper">
    <div class="avatar assistant-avatar">
      <span>{{ emoji }}</span>
    </div>
    <div class="typing-bubble">
      <span class="dot"></span>
      <span class="dot"></span>
      <span class="dot"></span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  emoji: { type: String, default: '🤖' }
})
</script>

<style scoped>
.typing-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
  animation: msgIn 0.3s ease;
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.assistant-avatar {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.2);
}

.typing-bubble {
  display: flex;
  gap: 5px;
  padding: 14px 20px;
  border-radius: 18px;
  border-top-left-radius: 6px;
  background: var(--card-bg);
  border: 1px solid rgba(251, 191, 36, 0.2);
  box-shadow: var(--shadow-sm);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--sun-yellow);
  animation: bounce 1.4s ease-in-out infinite both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
