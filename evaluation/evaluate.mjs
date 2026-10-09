import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const baseUrl = (process.argv[2] ?? 'http://127.0.0.1:8080').replace(/\/$/, '')
const caseFile = process.argv[3] ? resolve(process.argv[3]) : new URL('./questions.json', import.meta.url)

function requireCondition(condition, message) {
  if (!condition) throw new Error(message)
}
function exactKeys(value, keys) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    && Object.keys(value).sort().join(',') === [...keys].sort().join(',')
}
async function json(url, options = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(70000) })
  requireCondition(response.ok, `${url} returned HTTP ${response.status}`)
  return response.json()
}

try {
  const cases = JSON.parse(await readFile(caseFile, 'utf8'))
  requireCondition(Array.isArray(cases) && cases.length > 0, 'Provide a nonempty JSON array of evaluation cases.')
  const kb = await json(`${baseUrl}/api/kb`)
  requireCondition(Array.isArray(kb), 'Knowledge-base endpoint must return an array.')
  const documents = new Map(kb.map(doc => [doc.id, doc]))
  const rows = []
  for (const test of cases) {
    requireCondition(typeof test.question === 'string' && ['answer', 'abstain'].includes(test.expected), 'Invalid evaluation case.')
    const body = await json(`${baseUrl}/ask`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ question: test.question }),
    })
    const contractValid = exactKeys(body, ['question', 'decision', 'answer', 'citations', 'debug'])
      && body.question === test.question && ['answer', 'abstain'].includes(body.decision)
      && typeof body.answer === 'string' && body.answer.length > 0 && Array.isArray(body.citations)
      && exactKeys(body.debug, ['retrieved_ids', 'support_score'])
      && Array.isArray(body.debug.retrieved_ids) && body.debug.retrieved_ids.every(id => typeof id === 'string' && documents.has(id))
      && Number.isFinite(body.debug.support_score) && body.debug.support_score >= 0 && body.debug.support_score <= 1
      && body.citations.every(c => exactKeys(c, ['id', 'title', 'snippet'])
        && typeof c.id === 'string' && typeof c.title === 'string' && typeof c.snippet === 'string')
    const citationsValid = contractValid && body.citations.every(c => {
      const source = documents.get(c.id)
      return source?.title === c.title && c.snippet.length > 0 && source.text.includes(c.snippet)
        && body.answer.includes(c.snippet) && body.debug.retrieved_ids.includes(c.id)
    })
    const cited = body.decision === 'answer' ? body.citations?.length > 0 : body.citations?.length === 0
    rows.push({ question: test.question, expected: test.expected, actual: body.decision,
      correct: body.decision === test.expected, contractValid, citationsValid,
      citationPresentWhenRequired: cited })
  }
  const answered = rows.filter(row => row.actual === 'answer')
  const rate = (count, total) => total ? Number((count / total).toFixed(4)) : null
  const report = {
    knowledgeBaseLoaded: true,
    documentCount: kb.length,
    caseCount: rows.length,
    decisionAccuracy: rate(rows.filter(row => row.correct).length, rows.length),
    answeredCitationPresenceRate: rate(answered.filter(row => row.citationPresentWhenRequired).length, answered.length),
    citationSourceMatchRate: rate(answered.filter(row => row.citationsValid).length, answered.length),
    jsonContractRate: rate(rows.filter(row => row.contractValid).length, rows.length),
    unsupportedAnsweredCount: rows.filter(row => row.expected === 'abstain' && row.actual === 'answer').length,
    results: rows,
  }
  console.log(JSON.stringify(report, null, 2))
  if (rows.some(row => !row.correct || !row.contractValid || !row.citationsValid || !row.citationPresentWhenRequired)) process.exitCode = 1
} catch (error) {
  console.error(`Evaluation failed: ${error.message}`)
  process.exitCode = 1
}
