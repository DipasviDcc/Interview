<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type Answer, type Article, type ServiceStatus } from './api'
import AnswerCard from './components/AnswerCard.vue'
import KnowledgeBaseView from './components/KnowledgeBaseView.vue'

const view = ref<'ask' | 'kb'>('ask')
const question = ref('')
const result = ref<Answer | null>(null)
const busy = ref(false)
const error = ref('')
const bootError = ref('')
const bootLoading = ref(true)
const articles = ref<Article[]>([])
const status = ref<ServiceStatus | null>(null)
const textarea = ref<HTMLTextAreaElement | null>(null)
const modeLabel = computed(() => status.value?.answerMode === 'openai'
  ? (status.value.openaiAvailable ? 'OpenAI with offline fallback' : 'Offline fallback · OpenAI unconfigured')
  : 'Source excerpts · no API required')

async function load() {
  bootLoading.value = true
  bootError.value = ''
  try {
    const [serviceStatus, kb] = await Promise.all([api.status(), api.articles()])
    status.value = serviceStatus
    articles.value = kb
  } catch (e) {
    bootError.value = e instanceof Error ? e.message : 'Could not load the service.'
  } finally {
    bootLoading.value = false
  }
}

async function submit() {
  if (busy.value || !question.value.trim() || bootLoading.value || bootError.value) return
  busy.value = true
  error.value = ''
  result.value = null
  try { result.value = await api.ask(question.value.trim()) }
  catch (e) { error.value = e instanceof Error ? e.message : 'Something went wrong. Please try again.' }
  finally { busy.value = false }
}

function chooseTopic(title: string) {
  view.value = 'ask'
  question.value = `${title}?`
  error.value = ''
  setTimeout(() => textarea.value?.focus(), 0)
}
onMounted(load)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="/" aria-label="Evidence home"><span class="brand-mark" aria-hidden="true">e<span>·</span></span><span>evidence<span class="brand-period">.</span></span></a>
      <div class="workspace-label">SUPPORT WORKSPACE</div>
      <nav aria-label="Main navigation">
        <button :class="['nav-item', { active: view === 'ask' }]" :aria-current="view === 'ask' ? 'page' : undefined" @click="view = 'ask'">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 5h14v11H9l-4 3V5Z" stroke="currentColor" stroke-width="1.5"/><path d="M8 9h8M8 12h5" stroke="currentColor" stroke-width="1.5"/></svg>Ask support<span aria-hidden="true">↗</span>
        </button>
        <button :class="['nav-item', { active: view === 'kb' }]" :aria-current="view === 'kb' ? 'page' : undefined" @click="view = 'kb'">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h6l2 2 2-2h6v14h-6l-2 2-2-2H4V5Z" stroke="currentColor" stroke-width="1.5"/><path d="M12 7v14" stroke="currentColor" stroke-width="1.5"/></svg>Knowledge base<span class="nav-count">{{ articles.length }}</span>
        </button>
        <a class="nav-item" href="/swagger-ui/index.html" target="_blank" rel="noopener noreferrer">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m8 7-5 5 5 5m8-10 5 5-5 5m-3-12-2 14" stroke="currentColor" stroke-width="1.5"/></svg>API reference<span aria-hidden="true">↗</span>
        </a>
      </nav>
      <div class="sidebar-note"><span class="note-symbol" aria-hidden="true">✳</span><h2>Every answer starts<br />with evidence.</h2><p>Clear answers when the sources support them. A clear handoff when they don’t.</p><div class="note-rule"></div><span class="small-label">GROUNDED BY DESIGN</span></div>
      <div class="sidebar-bottom"><span :class="['status-dot', { offline: !!bootError }]" aria-hidden="true"></span>{{ bootLoading ? 'Connecting to service' : bootError ? 'Service unavailable' : 'Knowledge base connected' }}</div>
    </aside>

    <main>
      <header class="topbar"><div><span class="breadcrumb">Workspace</span><span class="breadcrumb-slash">/</span>{{ view === 'ask' ? 'Ask support' : 'Knowledge base' }}</div><span class="local-badge"><span aria-hidden="true">◈</span> LOCAL KNOWLEDGE</span></header>
      <div class="main-content">
        <div class="page-heading"><div class="eyebrow"><span class="heading-line"></span>{{ view === 'ask' ? 'ANSWERS YOU CAN TRACE' : 'THE SOURCE OF EVERY ANSWER' }}</div><h1>{{ view === 'ask' ? 'Support, with sources.' : 'Your knowledge base.' }}</h1><p>{{ view === 'ask' ? 'Ask a question. Get an answer grounded in the articles you can inspect.' : 'Explore the original articles behind your support answers.' }}</p></div>

        <div v-if="bootError" class="error-banner" role="alert"><div><strong>Service unavailable</strong><p>{{ bootError }}</p></div><button class="secondary-button" @click="load">Retry connection</button></div>
        <div v-else-if="bootLoading" class="loading-connect" role="status"><span class="spinner"></span>Connecting to your knowledge base…</div>

        <template v-else-if="view === 'ask'">
          <div class="ask-layout">
            <div class="conversation-column">
              <form class="question-card" @submit.prevent="submit">
                <div class="question-top"><label for="question">What can we help you with?</label><span class="small-label">ASK A QUESTION</span></div>
                <textarea id="question" ref="textarea" v-model="question" :disabled="busy" maxlength="1000" placeholder="For example, how do I reset my password?" rows="4" @keydown.ctrl.enter.prevent="submit" @keydown.meta.enter.prevent="submit"></textarea>
                <div class="question-footer"><span>{{ question.length }}/1000 <span class="keyboard-hint">· Ctrl + Enter to send</span></span><button class="primary-button" type="submit" :disabled="busy || !question.trim()"><span v-if="busy" class="spinner"></span>{{ busy ? 'Checking sources…' : 'Find an answer' }}<span v-if="!busy" aria-hidden="true">↗</span></button></div>
              </form>
              <div v-if="articles.length" class="topic-suggestions"><span>EXPLORE A TOPIC</span><button v-for="article in articles.slice(0, 3)" :key="article.id" :disabled="busy" @click="chooseTopic(article.title)">{{ article.title }} <span aria-hidden="true">+</span></button></div>
              <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
              <div aria-live="polite" aria-atomic="true">
                <div v-if="busy" class="answer-loading" role="status"><span class="spinner"></span><div><strong>Looking for supporting evidence</strong><p>Retrieving articles and checking whether they support your question.</p></div></div>
                <AnswerCard v-else-if="result" :result="result" />
                <div v-else class="empty-answer"><div class="empty-icon" aria-hidden="true"><svg viewBox="0 0 48 48" fill="none"><path d="M12 10h24v28H12zM18 18h12M18 24h12M18 30h7" stroke="currentColor" stroke-width="1.6"/><circle cx="35" cy="34" r="8" fill="var(--paper)" stroke="currentColor" stroke-width="1.6"/><path d="m31 34 3 3 5-6" stroke="currentColor" stroke-width="1.6"/></svg></div><h2>A little clarity starts here.</h2><p>Your answer and its supporting sources will appear here.<br />If the evidence is missing, we’ll say so.</p></div>
              </div>
            </div>
            <aside class="context-column" aria-label="About this service">
              <section class="kb-summary"><div class="summary-icon" aria-hidden="true">▤</div><span class="eyebrow">YOUR KNOWLEDGE BASE</span><div class="article-count">{{ articles.length }}<span>articles ready</span></div><p>The service searches these articles before answering a question.</p><button class="text-button" @click="view = 'kb'">Browse the articles <span aria-hidden="true">↗</span></button></section>
              <section class="how-it-works"><span class="eyebrow">HOW IT WORKS</span><ol><li><span>01</span><div><strong>Find the evidence</strong><p>Retrieve passages related to your question.</p></div></li><li><span>02</span><div><strong>Check the support</strong><p>Check the requested details against the sources.</p></div></li><li><span>03</span><div><strong>Answer or hand off</strong><p>Share a cited answer or direct you to human support.</p></div></li></ol></section>
            </aside>
          </div>
        </template>
        <KnowledgeBaseView v-else :articles="articles" @ask="chooseTopic" />
        <footer class="page-footer"><span>Evidence over assumptions.</span><span>{{ modeLabel }}</span></footer>
      </div>
    </main>
  </div>
</template>
