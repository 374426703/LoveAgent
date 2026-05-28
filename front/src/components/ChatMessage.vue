<template>
  <div class="message-wrapper" :class="role">
    <div v-if="role === 'assistant'" class="avatar assistant-avatar">
      <span>{{ assistantEmoji }}</span>
    </div>

    <div class="message-bubble" :class="role">
      <div class="bubble-content" v-html="renderedContent"></div>
      <div class="bubble-time">{{ formattedTime }}</div>
    </div>

    <div v-if="role === 'user'" class="avatar user-avatar">
      <span>👤</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, required: true },
  role: { type: String, required: true },
  timestamp: { type: [Date, Number], default: () => Date.now() },
  assistantEmoji: { type: String, default: '🤖' }
})

function escapeHtml(text) {
  const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }
  return text.replace(/[&<>"']/g, c => map[c])
}

const renderedContent = computed(() => {
  let text = escapeHtml(props.content)

  text = text.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
    return `<pre class="code-block"><code>${escapeHtml(code.trim())}</code></pre>`
  })

  text = text.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')

  text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>')

  text = text.replace(/~~([^~]+)~~/g, '<del>$1</del>')

  text = text.replace(/^### (.+)$/gm, '<h3 class="md-h3">$1</h3>')
  text = text.replace(/^## (.+)$/gm, '<h2 class="md-h2">$1</h2>')
  text = text.replace(/^# (.+)$/gm, '<h1 class="md-h1">$1</h1>')

  text = text.replace(/^- (.+)$/gm, '<li class="md-li">$1</li>')
  text = text.replace(/(<li class="md-li">.*<\/li>\n?)+/g, '<ul class="md-ul">$&</ul>')

  text = text.replace(/^> (.+)$/gm, '<blockquote class="md-quote">$1</blockquote>')

  text = text.replace(/\n\n+/g, '</p><p class="md-p">')
  text = text.replace(/\n/g, '<br/>')
  text = '<p class="md-p">' + text + '</p>'

  return text
})

const formattedTime = computed(() => {
  const d = new Date(props.timestamp)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})
</script>

<style scoped>
.message-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: flex-start;
  animation: msgIn 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes msgIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-wrapper.user {
  flex-direction: row-reverse;
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
  transition: transform 0.2s ease;
}

.assistant-avatar {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.2);
}

.user-avatar {
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.message-bubble {
  max-width: 72%;
  padding: 14px 18px;
  border-radius: 18px;
  position: relative;
}

.message-bubble.assistant {
  background: var(--card-bg);
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-top-left-radius: 6px;
  box-shadow: var(--shadow-sm);
}

.message-bubble.user {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-top-right-radius: 6px;
  box-shadow: 0 2px 12px rgba(251, 191, 36, 0.15);
}

.bubble-content {
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--text-primary);
  word-break: break-word;
}

.bubble-time {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-light);
  text-align: right;
}

/* Markdown styles */
.bubble-content :deep(.md-p) {
  margin: 0 0 6px;
}

.bubble-content :deep(.md-p:last-child) {
  margin-bottom: 0;
}

.bubble-content :deep(.md-h1),
.bubble-content :deep(.md-h2),
.bubble-content :deep(.md-h3) {
  margin: 12px 0 8px;
  font-weight: 700;
  line-height: 1.4;
}

.bubble-content :deep(.md-h1) { font-size: 20px; }
.bubble-content :deep(.md-h2) { font-size: 17px; }
.bubble-content :deep(.md-h3) { font-size: 15px; }

.bubble-content :deep(.md-ul) {
  margin: 6px 0;
  padding-left: 20px;
}

.bubble-content :deep(.md-li) {
  margin-bottom: 3px;
  list-style-type: disc;
}

.bubble-content :deep(.md-quote) {
  border-left: 3px solid var(--sun-yellow);
  padding: 6px 14px;
  margin: 8px 0;
  background: rgba(251, 191, 36, 0.08);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
  font-style: italic;
}

.bubble-content :deep(.code-block) {
  display: block;
  background: #1e293b;
  color: #e2e8f0;
  padding: 14px 18px;
  border-radius: 12px;
  margin: 10px 0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.bubble-content :deep(.inline-code) {
  background: rgba(251, 191, 36, 0.12);
  color: #d97706;
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 0.9em;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.bubble-content :deep(strong) {
  font-weight: 700;
  color: #92400e;
}

.bubble-content :deep(em) {
  font-style: italic;
}

.bubble-content :deep(del) {
  opacity: 0.6;
}

@media (max-width: 600px) {
  .message-bubble {
    max-width: 85%;
  }
}
</style>
