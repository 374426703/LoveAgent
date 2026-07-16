<template>
  <div class="admin-page">
    <nav class="top-bar">
      <div class="top-bar-inner">
        <div class="brand">
          <router-link to="/" class="back-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </router-link>
          <span class="brand-emoji">⚙️</span>
          <span class="brand-name">管理控制台</span>
        </div>
        <div class="user-area">
          <span class="user-tag">{{ user?.nickname || user?.username }}</span>
        </div>
      </div>
    </nav>

    <div class="admin-wrapper">
      <div class="tab-bar">
        <button :class="['tab', { active: activeTab === 'docs' }]" @click="activeTab = 'docs'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          文档管理
        </button>
        <button :class="['tab', { active: activeTab === 'search' }]" @click="activeTab = 'search'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          知识库查询
        </button>
      </div>

      <!-- 文档管理 -->
      <div v-if="activeTab === 'docs'" class="panel">
        <div class="panel-header">
          <h2 class="panel-title">知识库文档</h2>
          <div class="panel-actions">
            <button class="btn btn-secondary" @click="handleReload" :disabled="reloading">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              {{ reloading ? '加载中...' : '从Classpath重载' }}
            </button>
          </div>
        </div>

        <div class="upload-zone" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
          <input ref="fileInput" type="file" accept=".md" @change="handleFileSelect" style="display:none" />
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="upload-icon">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <p class="upload-text">拖拽 Markdown 文件到此处，或<span class="upload-link">点击选择文件</span></p>
          <p class="upload-hint">仅支持 .md 格式文件</p>
        </div>

        <div v-if="uploading" class="upload-progress">正在处理文档，请稍候...</div>

        <div v-if="message" :class="['msg', messageType]">{{ message }}</div>

        <div class="doc-table-wrap" v-if="documents.length > 0">
          <table class="doc-table">
            <thead>
              <tr>
                <th>文件名</th>
                <th>片段数</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in documents" :key="doc.id">
                <td class="doc-name">{{ doc.title || doc.filename }}</td>
                <td>{{ doc.chunkCount }}</td>
                <td>{{ formatTime(doc.createdAt) }}</td>
                <td>
                  <button class="btn btn-danger-sm" @click="handleDelete(doc.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>暂无文档，请上传 Markdown 文件</p>
        </div>
      </div>

      <!-- 知识库查询 -->
      <div v-if="activeTab === 'search'" class="panel">
        <div class="search-box">
          <div class="search-input-wrap">
            <input v-model="searchQuery" type="text" placeholder="输入查询内容搜索，留空则列出全部数据..." @keyup.enter="handleSearch(1)" />
            <button class="btn btn-primary" @click="handleSearch(1)" :disabled="searching">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              {{ searching ? '搜索中...' : '查询' }}
            </button>
          </div>
        </div>

        <div v-if="searchMessage" :class="['msg', searchMsgType]">{{ searchMessage }}</div>

        <div v-if="searchTotal > 0" class="results">
          <div class="results-header">
            共 {{ searchTotal }} 条，第 {{ searchPage }} / {{ totalPages }} 页
          </div>
          <div class="result-card" v-for="(item, idx) in searchResults" :key="item.id || idx">
            <div class="result-rank">#{{ (searchPage - 1) * searchPageSize + idx + 1 }}</div>
            <div class="result-body">
              <div class="result-content" :class="{ expanded: item._expanded }">{{ item._expanded ? item.fullContent : item.content }}</div>
              <button v-if="item.content !== item.fullContent" class="expand-btn" @click="item._expanded = !item._expanded">
                {{ item._expanded ? '收起' : '展开全文' }}
              </button>
              <div class="result-meta" v-if="item.metadata">
                <span class="meta-tag" v-for="(v, k) in item.metadata" :key="k">{{ k }}: {{ v }}</span>
              </div>
            </div>
          </div>

          <div class="pagination" v-if="totalPages > 1">
            <button :disabled="searchPage <= 1" @click="handleSearch(searchPage - 1)">上一页</button>
            <span v-for="p in pageNumbers" :key="p">
              <button v-if="p === '...'" disabled class="ellipsis">...</button>
              <button v-else :class="{ active: p === searchPage }" @click="handleSearch(p)">{{ p }}</button>
            </span>
            <button :disabled="searchPage >= totalPages" @click="handleSearch(searchPage + 1)">下一页</button>
          </div>
        </div>
        <div v-else-if="searched" class="empty-state">
          <p>未找到数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth.js'
import { getAll, uploadDoc, deleteDoc, searchKnowledge, reloadKnowledge } from '../api/index.js'

const { user } = useAuth()

const activeTab = ref('docs')
const fileInput = ref(null)

const documents = ref([])
const uploading = ref(false)
const reloading = ref(false)
const message = ref('')
const messageType = ref('')

const searchQuery = ref('')
const searchPage = ref(1)
const searchPageSize = ref(10)
const searchTotal = ref(0)
const searchResults = ref([])
const searching = ref(false)
const searched = ref(false)
const searchMessage = ref('')
const searchMsgType = ref('')

const totalPages = computed(() => Math.ceil(searchTotal.value / searchPageSize.value) || 1)

const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = searchPage.value
  const pages = []
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})

onMounted(() => {
  loadDocuments()
})

async function loadDocuments() {
  try {
    const res = await getAll('/api/admin/knowledge/documents')
    documents.value = res.data || []
  } catch {}
}

function triggerUpload() {
  fileInput.value.click()
}

async function handleFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  await doUpload(file)
  fileInput.value.value = ''
}

function handleDrop(e) {
  const file = e.dataTransfer.files[0]
  if (file) doUpload(file)
}

async function doUpload(file) {
  uploading.value = true
  message.value = ''
  try {
    const res = await uploadDoc(file)
    if (res.code === 0) {
      showMsg('上传成功', 'success')
      await loadDocuments()
    } else {
      showMsg(res.message || '上传失败', 'error')
    }
  } catch {
    showMsg('上传失败，请重试', 'error')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(id) {
  if (!confirm('确定要删除该文档吗？')) return
  try {
    const res = await deleteDoc(id)
    if (res.code === 0) {
      showMsg('删除成功', 'success')
      await loadDocuments()
    } else {
      showMsg(res.message || '删除失败', 'error')
    }
  } catch {
    showMsg('删除失败', 'error')
  }
}

async function handleReload() {
  reloading.value = true
  try {
    const res = await reloadKnowledge()
    if (res.code === 0) {
      showMsg('Classpath 文档重新加载完成', 'success')
      await loadDocuments()
    } else {
      showMsg(res.message || '加载失败', 'error')
    }
  } catch {
    showMsg('加载失败', 'error')
  } finally {
    reloading.value = false
  }
}

async function handleSearch(page = 1) {
  searchPage.value = page
  searching.value = true
  searched.value = true
  searchMessage.value = ''
  searchResults.value = []
  searchTotal.value = 0
  try {
    const res = await searchKnowledge(searchQuery.value, page, searchPageSize.value)
    if (res.code === 0) {
      searchResults.value = (res.data?.records || []).map(r => ({ ...r, _expanded: false }))
      searchTotal.value = res.data?.total || 0
    } else {
      searchMsgType.value = 'error'
      searchMessage.value = res.message || '查询失败'
    }
  } catch {
    searchMsgType.value = 'error'
    searchMessage.value = '查询失败，请重试'
  } finally {
    searching.value = false
  }
}

function showMsg(msg, type) {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--card-border);
}

.top-bar-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(251, 191, 36, 0.1);
  color: var(--sun-orange);
  transition: all 0.2s;
}
.back-link:hover { background: rgba(251, 191, 36, 0.2); transform: translateX(-2px); }
.back-link svg { width: 18px; height: 18px; }

.brand-emoji { font-size: 22px; }
.brand-name { font-size: 17px; font-weight: 700; color: var(--text-primary); }

.user-tag {
  padding: 6px 14px;
  border-radius: 10px;
  background: rgba(251, 191, 36, 0.1);
  font-size: 13px;
  font-weight: 600;
  color: var(--sun-orange);
}

.admin-wrapper {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px 48px;
  flex: 1;
}

.tab-bar {
  display: flex;
  gap: 4px;
  background: rgba(251, 191, 36, 0.06);
  border: 1px solid rgba(251, 191, 36, 0.1);
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 28px;
}

.tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  border: none;
  border-radius: 11px;
  background: none;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: var(--text-light);
  cursor: pointer;
  transition: all 0.25s;
}
.tab svg { width: 16px; height: 16px; }
.tab.active {
  background: white;
  color: var(--sun-orange);
  box-shadow: 0 2px 10px rgba(251, 191, 36, 0.15);
}

.panel { animation: fadeIn 0.3s ease; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.panel-title { font-size: 18px; font-weight: 700; }

.panel-actions { display: flex; gap: 10px; }

.btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.btn svg { width: 15px; height: 15px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-primary {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: white;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.3);
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(245, 158, 11, 0.4); }

.btn-secondary {
  background: rgba(251, 191, 36, 0.1);
  color: var(--sun-orange);
  border: 1px solid rgba(251, 191, 36, 0.2);
}
.btn-secondary:hover:not(:disabled) { background: rgba(251, 191, 36, 0.2); }

.btn-danger-sm {
  padding: 5px 12px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.06);
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-danger-sm:hover { background: rgba(239, 68, 68, 0.12); }

.upload-zone {
  border: 2px dashed rgba(251, 191, 36, 0.3);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}
.upload-zone:hover {
  border-color: var(--sun-orange);
  background: rgba(251, 191, 36, 0.04);
}
.upload-icon { width: 40px; height: 40px; color: var(--sun-orange); margin-bottom: 12px; }
.upload-text { font-size: 15px; color: var(--text-secondary); margin-bottom: 6px; }
.upload-link { color: var(--sun-orange); font-weight: 600; }
.upload-hint { font-size: 12px; color: var(--text-light); }
.upload-progress {
  text-align: center;
  padding: 16px;
  margin-bottom: 20px;
  background: rgba(251, 191, 36, 0.08);
  border-radius: 12px;
  font-size: 14px;
  color: var(--sun-orange);
  font-weight: 500;
}

.msg { padding: 10px 14px; border-radius: 12px; margin-bottom: 16px; font-size: 13px; font-weight: 500; }
.msg.success { background: rgba(22, 163, 74, 0.08); color: #16a34a; border: 1px solid rgba(22, 163, 74, 0.2); }
.msg.error { background: rgba(239, 68, 68, 0.06); color: #dc2626; border: 1px solid rgba(239, 68, 68, 0.15); }

.doc-table-wrap {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  overflow: hidden;
}
.doc-table { width: 100%; border-collapse: collapse; }
.doc-table th {
  text-align: left;
  padding: 12px 20px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-light);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: rgba(251, 191, 36, 0.04);
  border-bottom: 1px solid var(--card-border);
}
.doc-table td {
  padding: 12px 20px;
  font-size: 14px;
  border-bottom: 1px solid rgba(251, 191, 36, 0.08);
}
.doc-table tr:last-child td { border-bottom: none; }
.doc-name { font-weight: 600; color: var(--text-primary); }

.empty-state { text-align: center; padding: 48px; color: var(--text-light); font-size: 14px; }

.search-box { margin-bottom: 24px; }
.search-input-wrap { display: flex; gap: 10px; margin-bottom: 12px; }
.search-input-wrap input {
  flex: 1;
  height: 48px;
  padding: 0 18px;
  border: 1.5px solid rgba(251, 191, 36, 0.15);
  border-radius: 14px;
  font-size: 15px;
  font-family: inherit;
  background: var(--card-bg);
  outline: none;
  transition: all 0.3s;
  color: var(--text-primary);
}
.search-input-wrap input:focus {
  border-color: var(--sun-orange);
  box-shadow: 0 0 0 4px rgba(251, 191, 36, 0.08);
}
.search-input-wrap input::placeholder { color: var(--text-light); }

.results-header { font-size: 13px; font-weight: 600; color: var(--text-light); margin-bottom: 14px; }

.result-card {
  display: flex;
  gap: 14px;
  padding: 18px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  margin-bottom: 10px;
  transition: all 0.2s;
}
.result-card:hover { border-color: var(--sun-orange); }

.result-rank {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fbbf24, #f97316);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.result-body { flex: 1; min-width: 0; }
.result-content { font-size: 14px; line-height: 1.7; color: var(--text-primary); white-space: pre-wrap; overflow: hidden; }
.result-content:not(.expanded) { max-height: 72px; }

.expand-btn {
  margin-top: 6px;
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--sun-orange);
  cursor: pointer;
  font-family: inherit;
}
.expand-btn:hover { text-decoration: underline; }

.result-meta { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.meta-tag {
  padding: 3px 10px;
  border-radius: 100px;
  font-size: 11px;
  background: rgba(251, 191, 36, 0.08);
  color: var(--text-secondary);
  border: 1px solid rgba(251, 191, 36, 0.15);
}

@media (max-width: 600px) {
  .admin-wrapper { padding: 16px 12px 32px; }
  .search-input-wrap { flex-direction: column; }
  .doc-table th, .doc-table td { padding: 10px 12px; font-size: 12px; }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}
.pagination button {
  padding: 6px 14px;
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 8px;
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.pagination button:hover:not(:disabled) { border-color: var(--sun-orange); color: var(--sun-orange); }
.pagination button.active { background: linear-gradient(135deg, #f59e0b, #f97316); color: white; border-color: transparent; }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination .ellipsis { border: none; background: none; cursor: default; color: var(--text-light); }
</style>
