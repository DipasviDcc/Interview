<script setup lang="ts">
import type { Answer } from '../api'
defineProps<{ result: Answer }>()
</script>

<template>
  <section class="answer-card" aria-labelledby="answer-title">
    <div class="answer-heading">
      <span class="eyebrow" id="answer-title">YOUR ANSWER</span>
      <span :class="['decision', result.decision]">
        <span aria-hidden="true">{{ result.decision === 'answer' ? '✓' : '↗' }}</span>
        {{ result.decision === 'answer' ? 'Supported by sources' : 'Insufficient information' }}
      </span>
    </div>
    <p class="asked-question">{{ result.question }}</p>
    <p class="answer-text">{{ result.answer }}</p>
    <div v-if="result.citations.length" class="citations">
      <div class="source-heading"><span class="eyebrow">SUPPORTING EVIDENCE</span><span>{{ result.citations.length }} source{{ result.citations.length === 1 ? '' : 's' }}</span></div>
      <details v-for="(citation, index) in result.citations" :key="`${citation.id}-${index}`" class="citation" :open="index === 0">
        <summary><span class="citation-number">{{ String(index + 1).padStart(2, '0') }}</span><span>{{ citation.title }}</span><code>{{ citation.id }}</code><span class="detail-chevron" aria-hidden="true">⌄</span></summary>
        <blockquote>{{ citation.snippet }}</blockquote>
      </details>
    </div>
    <p v-else class="abstain-note">The available articles do not sufficiently support this request. Human support can help with the missing details.</p>
    <details class="debug-panel">
      <summary>Retrieval details <span aria-hidden="true">+</span></summary>
      <dl>
        <div><dt>Retrieved articles</dt><dd>{{ result.debug.retrieved_ids.join(', ') || 'None' }}</dd></div>
        <div><dt>Support score</dt><dd>{{ result.debug.support_score.toFixed(4) }}</dd></div>
      </dl>
      <p>This score measures lexical support. It is not a probability that an answer is correct.</p>
    </details>
  </section>
</template>
