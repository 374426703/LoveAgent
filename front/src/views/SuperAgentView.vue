<template>
  <div class="chat-page super-page">
    <ConversationSidebar
      :conversations="conversations"
      :active-id="currentChatId"
      :visible="sidebarVisible"
      @new-conversation="newConversation"
      @select-conversation="selectConversation"
      @delete-conversation="deleteConversation"
      @toggle-sidebar="sidebarVisible = !sidebarVisible"
    />

    <div class="chat-main">
      <AppHeader
        title="超级特工"
        emoji="🦸"
        back-title="返回首页"
        :is-connected="true"
      />

      <div class="chat-container" ref="chatContainer">
        <div v-if="messages.length === 0" class="welcome-area">
          <div class="welcome-emoji">🦸</div>
          <h2 class="welcome-title">欢迎来到超级特工</h2>
          <p class="welcome-desc">我是你的全能 AI 助手，可以联网搜索、操作文件、生成 PDF、执行命令，帮你完成各种复杂任务</p>
          <div class="quick-prompts">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              class="quick-prompt-btn"
              @click="sendMessage(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </div>

        <div class="messages-list">
          <ChatMessage
            v-for="msg in messages"
            :key="msg.id"
            :content="msg.content"
            :role="msg.role"
            :timestamp="msg.timestamp"
            assistant-emoji="🦸"
          />
          <TypingIndicator v-if="isStreaming" emoji="🦸" />
        </div>
      </div>

      <ChatInput
        v-model="inputMessage"
        :disabled="isStreaming"
        placeholder="输入任务，超级特工帮你搞定..."
        @send="sendMessage"
        @stop="stopStreaming"
      />
    </div>

    <ConfirmModal
      :visible="showCreatePrompt"
      title="请先创建对话"
      message="发送消息前需要先创建一个新对话，是否立即创建？"
      emoji="💬"
      confirm-text="立即创建"
      cancel-text="取消"
      :show-cancel="true"
      @confirm="handleCreateAndSend"
      @cancel="showCreatePrompt = false"
    />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import ChatMessage from '../components/ChatMessage.vue'
import ChatInput from '../components/ChatInput.vue'
import TypingIndicator from '../components/TypingIndicator.vue'
import ConversationSidebar from '../components/ConversationSidebar.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { chatWithSuperAgent, apiListConversations, apiCreateConversation, apiGetConversationMessages, apiDeleteConversation } from '../api/index.js'

const chatContainer = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isStreaming = ref(false)
const sidebarVisible = ref(true)
const conversations = ref([])
const currentChatId = ref('')
const showCreatePrompt = ref(false)
let pendingMessage = ''
let currentSSE = null

// Typewriter engine
const TYPING_SPEED = 22
const CHARS_PER_TICK = 4
let twTimer = null
let twBuffer = ''
let twMsgId = null

function twTick() {
  const msg = twMsgId ? messages.value.find(m => m.id === twMsgId) : null
  if (!msg || twBuffer.length === 0) { twTimer = null; return }
  const batch = twBuffer.slice(0, CHARS_PER_TICK)
  twBuffer = twBuffer.slice(CHARS_PER_TICK)
  msg.content += batch
  scrollToBottom()
  twTimer = setTimeout(twTick, TYPING_SPEED)
}

function twFeed(msgId, chunk) {
  twMsgId = msgId
  twBuffer += chunk
  if (!twTimer) twTimer = setTimeout(twTick, TYPING_SPEED)
}

function twFlush() {
  if (twTimer) { clearTimeout(twTimer); twTimer = null }
  const msg = twMsgId ? messages.value.find(m => m.id === twMsgId) : null
  if (msg && twBuffer.length > 0) {
    msg.content += twBuffer
    twBuffer = ''
    scrollToBottom()
  }
  twMsgId = null
}

const quickPrompts = [
  '🔍 帮我搜索一下今天的科技新闻',
  '📄 生成一份关于AI发展趋势的PDF报告',
  '💻 分析这个项目的技术架构',
  '🌐 访问某个网站并总结主要内容',
  '📊 帮我整理一份数据分析方案',
  '🧠 用ReAct模式思考一个复杂问题'
]

onMounted(() => {
  loadConversations()
})

async function loadConversations() {
  try {
    const res = await apiListConversations('super_agent')
    if (res.code === 0) conversations.value = res.data
  } catch {}
}

async function newConversation(title = '新对话') {
  messages.value = []
  try {
    const res = await apiCreateConversation('super_agent', title)
    if (res.code === 0) {
      currentChatId.value = res.data.id
      await loadConversations()
    }
  } catch {}
}

async function selectConversation(conv) {
  if (isStreaming.value) return
  currentChatId.value = conv.id
  try {
    const res = await apiGetConversationMessages(conv.id)
    if (res.code === 0) {
      messages.value = (res.data.messages || []).map(m => ({
        id: 'm-' + Math.random().toString(36).slice(2),
        content: m.content,
        role: m.role,
        timestamp: m.timestamp
      }))
      scrollToBottom()
    }
  } catch {}
}

async function deleteConversation(conv) {
  try {
    await apiDeleteConversation(conv.id)
    if (currentChatId.value === conv.id) {
      currentChatId.value = ''
      messages.value = []
    }
    await loadConversations()
  } catch {}
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

async function handleCreateAndSend() {
  showCreatePrompt.value = false
  const title = pendingMessage.length > 30 ? pendingMessage.slice(0, 30) + '...' : pendingMessage
  await newConversation(title)
  if (pendingMessage && currentChatId.value) {
    sendMessage(pendingMessage)
    pendingMessage = ''
  }
}

function sendMessage(text) {
  if (!text.trim() || isStreaming.value) return

  if (!currentChatId.value) {
    pendingMessage = text.trim()
    showCreatePrompt.value = true
    return
  }

  const userMsg = {
    id: 'u-' + Date.now(),
    content: text.trim(),
    role: 'user',
    timestamp: Date.now()
  }
  messages.value.push(userMsg)
  scrollToBottom()

  const assistantMsg = {
    id: 'a-' + Date.now(),
    content: '',
    role: 'assistant',
    timestamp: Date.now()
  }
  messages.value.push(assistantMsg)
  isStreaming.value = true
  scrollToBottom()

  const chatId = currentChatId.value

  currentSSE = chatWithSuperAgent(
    text.trim(),
    chatId,
    (chunk) => { twFeed(assistantMsg.id, chunk) },
    () => {
      twFlush()
      isStreaming.value = false
      currentSSE = null
      if (!assistantMsg.content) {
        assistantMsg.content = '（未收到回复，请稍后重试）'
      }
      loadConversations()
    },
    (err) => {
      console.error('SSE error:', err)
      twFlush()
      isStreaming.value = false
      currentSSE = null
      if (!assistantMsg.content) {
        assistantMsg.content = '抱歉，连接出现了问题，请稍后重试 🦸'
      }
    }
  )
}

function stopStreaming() {
  if (currentSSE) {
    currentSSE.abort()
    currentSSE = null
    twFlush()
    isStreaming.value = false
    loadConversations()
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(180deg,
    rgba(255, 251, 235, 0.5) 0%,
    rgba(254, 243, 199, 0.4) 100%
  );
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 48px 20px 0;
}

.welcome-emoji {
  font-size: 56px;
  margin-bottom: 16px;
  animation: floatEmoji 3s ease-in-out infinite;
}

@keyframes floatEmoji {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-8px) rotate(3deg); }
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-desc {
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 400px;
  line-height: 1.7;
  margin-bottom: 28px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 460px;
}

.quick-prompt-btn {
  padding: 10px 18px;
  border-radius: 100px;
  border: 1px solid rgba(251, 191, 36, 0.3);
  background: rgba(254, 243, 199, 0.6);
  color: #b45309;
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.quick-prompt-btn:hover {
  background: rgba(251, 191, 36, 0.15);
  border-color: rgba(251, 191, 36, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.2);
}

.messages-list {
  max-width: 720px;
  margin: 0 auto;
}
</style>
