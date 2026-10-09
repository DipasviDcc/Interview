<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Article } from '../api'
const props = defineProps<{ articles: Article[] }>()
const emit = defineEmits<{ ask: [title: string] }>()
const search = ref('')
const filtered = computed(() => props.articles.filter(article =>
  `${article.title} ${article.id} ${article.text}`.toLowerCase().includes(search.value.toLowerCase().trim())))
</script>

<template>
  <section class="kb-view" aria-label="Read-only knowledge base">
    <div class="kb-toolbar">
      <label class="search-box"><span aria-hidden="true">⌕</span><input v-model="search" type="search" placeholder="Search articles…" aria-label="Search knowledge base" /></label>
      <span class="muted">{{ filtered.length }} of {{ articles.length }} articles</span>
    </div>
    <div v-if="!filtered.length" class="empty-search"><h2>No articles found</h2><p>Try a different search term.</p></div>
    <article v-for="(article, index) in filtered" :key="article.id" class="kb-article">
      <div class="kb-article-top"><span class="eyebrow">ARTICLE {{ String(index + 1).padStart(2, '0') }}</span><code>{{ article.id }}</code></div>
      <h2>{{ article.title }}</h2>
      <p>{{ article.text }}</p>
      <button type="button" class="text-button" @click="emit('ask', article.title)">Ask about this topic <span aria-hidden="true">↗</span></button>
    </article>
    <p class="kb-footnote">Read-only view of the articles loaded by the service. Questions are answered from these sources.</p>
  </section>
</template>
