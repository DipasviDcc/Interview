export interface Article { id: string; title: string; text: string }
export interface Citation { id: string; title: string; snippet: string }
export interface Answer {
  question: string
  decision: 'answer' | 'abstain'
  answer: string
  citations: Citation[]
  debug: { retrieved_ids: string[]; support_score: number }
}
export interface ServiceStatus {
  status: string
  documentCount: number
  answerMode: 'extractive' | 'openai'
  openaiAvailable: boolean
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(url, { ...options, signal: AbortSignal.timeout(70000) })
  } catch {
    throw new Error('Could not reach the support service. Check that the backend is running and try again.')
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.error ?? `The service returned an error (${response.status}). Please try again.`)
  }
  return response.json() as Promise<T>
}

export const api = {
  status: () => request<ServiceStatus>('/api/status'),
  articles: () => request<Article[]>('/api/kb'),
  ask: (question: string) => request<Answer>('/ask', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ question }),
  }),
}
