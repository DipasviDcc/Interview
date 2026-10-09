# Evidence Support

A small Spring Boot + Vue support service that reads `kb.json`, retrieves evidence with TF-IDF, and either returns a cited answer or abstains. It runs without an API key. An optional OpenAI mode uses the official Java SDK and Responses API.

The Vue application includes a question form, answer and abstention states, expandable citations, retrieval details, a searchable **read-only knowledge-base viewer**, and a link to **Swagger UI**.

## Requirements

- JDK 17 or later (tested with Java 17).
- Node.js 22.12 or later and npm for the Vue frontend and HTTP evaluation script.
- Internet access on the first build to download Maven/npm dependencies. Maven is bootstrapped by the checked-in wrapper; a separate Maven installation is unnecessary.
- An OpenAI API key only when using OpenAI mode. No secrets are needed for tests or the core service.

Pinned dependencies: Spring Boot 3.5.16, OpenAI Java 4.78.1, springdoc 2.8.17, Vue 3.5.43, Vite 8.3.4. Frontend transitive versions are recorded in `frontend/package-lock.json`.

## Run from a clean checkout

Run backend commands from the repository root, where `kb.json` lives.

**Windows / PowerShell — terminal 1:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Windows / PowerShell — terminal 2:**

```powershell
cd frontend
npm ci
npm run dev
```

**macOS / Linux — terminal 1:**

```sh
sh ./mvnw spring-boot:run
```

**macOS / Linux — terminal 2:**

```sh
cd frontend
npm ci
npm run dev
```

Open:

- Frontend: <http://127.0.0.1:5173>
- Swagger UI: <http://127.0.0.1:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://127.0.0.1:8080/v3/api-docs>
- Read-only KB API: <http://127.0.0.1:8080/api/kb>
- Readiness and mode: <http://127.0.0.1:8080/api/status>

Vite proxies API requests to the backend, so no CORS setup is needed. The API key is never sent to the frontend. The service and Vite bind to loopback by default. Stop either server with Ctrl+C in its terminal.

On Windows, stop a server launched with `java -jar` before rebuilding that JAR; the running process holds a file lock. Development with `spring-boot:run` avoids locking the packaged JAR.

## Validate

The backend command loads the repository `kb.json`, tests answer/abstain behavior, validates citations and the JSON contract, tests replacement KB files, and packages a runnable JAR. API behavior tests use a separate sample fixture so a valid replacement KB does not invalidate their expected sample answers.

```powershell
# Repository root; Windows
.\mvnw.cmd verify

# Repository root; macOS/Linux equivalent
sh ./mvnw verify
```

Validate TypeScript and build the Vue application:

```sh
cd frontend
npm ci
npm run build
```

With the backend running, run the eight-case HTTP evaluation from the repository root:

```sh
node evaluation/evaluate.mjs
```

The evaluation reports decision accuracy, citation presence for answered questions, citation/source matching, JSON contract validity, and the number of unsupported questions incorrectly answered. It exits nonzero on a failure. Citation matching checks exact source text; the metric does not independently measure semantic relevance.

For a different server or replacement KB, supply your own cases with `question` and `expected` (`answer` or `abstain`):

```sh
node evaluation/evaluate.mjs http://127.0.0.1:8080 path/to/cases.json
```

The bundled eight cases target the supplied sample KB. Replace their expected questions when evaluating unrelated KB content. The evaluator always fetches the currently loaded KB to verify citation membership and snippets.

The OpenAI integration test uses a local HTTP stub and the **real official SDK**, checking the Responses API payload, strict structured format, and rejection of fabricated citations. Automated validation makes no paid API calls.

## Enable OpenAI

**PowerShell:**

```powershell
$env:ANSWER_MODE = "openai"
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_MODEL = "gpt-4.1-mini"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```sh
export ANSWER_MODE=openai
export OPENAI_API_KEY=your-api-key
export OPENAI_MODEL=gpt-4.1-mini
sh ./mvnw spring-boot:run
```

Use a model available to your API account that supports Responses and Structured Outputs. The model name is configurable. `.env.example` documents the settings; `.env` files are **not automatically loaded** by Java or these commands.

OpenAI receives only the current question and approved retrieved excerpts. Requests use `store=false`, a 20-second per-request timeout, at most one SDK retry, and a 1,200-token output limit. There is no conversation memory or web search.

To make citation grounding directly checkable, **both modes return verbatim excerpts**. In OpenAI mode the model selects complete approved excerpts into a structured claim list. It does not freely paraphrase the facts. The backend rejects altered quotations, missing subquestions, duplicate claims, and unknown document IDs. This deliberately trades conversational wording for verifiable evidence.

If the key is missing or the provider fails, the service uses the checked extractive composer. A model refusal or a well-formed but invalid claim list causes abstention. Parsing failures and incomplete provider responses use the same validated offline fallback as provider errors. Logs record the exception type without logging API keys, question text, or provider response bodies. `/api/status` reports configuration availability, not a live credential validity check.

Official references: [Java SDK](https://developers.openai.com/api/reference/java), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs), [configured default model](https://developers.openai.com/api/docs/models/gpt-4.1-mini).

## Replace the knowledge base

Replace the root `kb.json` or set `KB_PATH`, then restart the backend:

```powershell
$env:KB_PATH = "D:\data\replacement.json"
.\mvnw.cmd spring-boot:run
```

```sh
KB_PATH=/path/to/replacement.json sh ./mvnw spring-boot:run
```

Schema:

```json
[
  { "id": "unique-id", "title": "Article title", "text": "Original support article text." }
]
```

IDs must be unique; all three fields must be nonblank strings. The loader fails clearly on missing/malformed files or invalid records. An empty array is valid and causes questions to abstain. The index is rebuilt from disk on each start; no answers or embeddings are precomputed. The files under `src/main/samplefile/` are original reference material and are not the runtime input.

The KB viewer shows the loaded snapshot. It cannot edit/upload articles. Restart the backend and refresh the UI to see a changed file.

## API contract

```http
POST /ask
Content-Type: application/json

{ "question": "How do I reset my password?" }
```

```json
{
  "question": "How do I reset my password?",
  "decision": "answer",
  "answer": "A verbatim excerpt from the retrieved article.",
  "citations": [
    { "id": "doc_1", "title": "Password reset", "snippet": "The same exact source excerpt." }
  ],
  "debug": {
    "retrieved_ids": ["doc_1"],
    "support_score": 0.8
  }
}
```

This example illustrates the schema; the service fills the answer, snippet, retrieval IDs, and score from the actual query.

**PowerShell request:**

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/ask -ContentType application/json -Body '{"question":"How do I reset my password?"}' | ConvertTo-Json -Depth 6
```

**curl request:**

```sh
curl -s http://127.0.0.1:8080/ask -H 'Content-Type: application/json' \
  -d '{"question":"How do I reset my password?"}'
```

Valid questions return HTTP 200 for either decision. An abstention has an empty citation list and a clear message suggesting human support. Missing, blank, non-string, or over-1,000-character questions and malformed JSON return HTTP 400 with an `error` string.

## Retrieval and support rules

```text
LOAD_KB → BUILD_TFIDF_INDEX
                    ↓
question → RETRIEVE → CHECK_SUPPORT → ANSWER_COMPOSITION → VALIDATE → answer
                           ↓                                  ↓
                        abstain                            abstain
```

1. Normalize Unicode, lowercase, remove common grammatical/question words, and apply small language-level word-form rules. Negation and numbers remain. There are no sample-question lookup tables or embedded policy answers.
2. Index each short FAQ article as a single document. Titles are included twice to boost topic matching. TF is `1 + ln(count)`; IDF is `1 + ln((N + 1)/(df + 1))`. L2-normalized vectors are ranked by cosine similarity. Unknown query terms retain weight rather than disappearing.
3. Retrieve up to three nonzero-scoring documents. A candidate needs cosine similarity **at least 0.12**.
4. Preserve complete, contiguous source text. Short articles are used whole, keeping caveats and conditions together. For articles over 1,200 characters, select a sentence-aligned contiguous window up to that limit with the highest query-term coverage. Instructions addressed to a model, and legal/tax advice passages, are excluded as answer evidence. No mid-sentence truncation is used.
5. Split explicit subquestions at question marks, `and`, or `also`. Every nonempty clause needs **at least 0.85 IDF-weighted term coverage in one candidate's body text**. Title overlap alone cannot authorize an answer. A supported first clause cannot compensate for an unsupported second clause.
6. All standalone numeric values and numeric time-unit pairs requested by a clause must occur in its evidence. Duration/expiry questions need an explicit time fact. A requested guarantee or certainty needs explicit matching wording without negation or uncertainty qualifiers. Explicit role/action questions (for example, “Can support …?”) need that actor and action in the same source sentence.
7. If multiple eligible sources differ in negation or contain different sets of numeric values, abstain conservatively.
8. Compose only after the gate passes. Each returned quote must exactly match a whole approved excerpt, its document must be retrieved, and its text must occur in the original file. Re-run support checks against only the returned excerpts. Then build the public answer and citations on the server.

For an accepted question, `support_score` is the minimum across clauses of `0.65 × coverage + 0.35 × retrieval_similarity`, rounded to four decimals. For a rejected clause it reports its best lexical coverage (or zero when no usable evidence/conflicting evidence exists). Additional fact checks can therefore abstain even when lexical coverage is high. **This diagnostic is not a probability or a calibrated confidence score.**

### Tradeoffs and limits

- The offline gate uses English lexical heuristics, not a general semantic entailment model. Unfamiliar paraphrases, compound wording, and valid questions can abstain. Simple conjunction splitting and numeric/negation conflict checks intentionally favor caution.
- Lexical overlap and the simple role/action guard cannot fully resolve actors, relationships, contradictory policies, or prompt injection in arbitrary untrusted text. The instruction filter is defense in depth, not a complete classifier. Use a curated support KB.
- Long-article sentence windows can lose distant context. Short self-contained FAQ articles are the intended input; conditions should stay in the same article/window as their facts.
- Exact source quotation prevents invented answer text; it does not independently prove that a source is correct or relevant. Tests cover the sample requirements and additional counterexamples rather than claiming universal grounding.
- Streaming is omitted so unvalidated drafts never reach the UI. No database or external vector service is needed.

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `KB_PATH` | `./kb.json` | External input file, relative to the process working directory |
| `ANSWER_MODE` | `extractive` | `extractive` or `openai` |
| `OPENAI_API_KEY` | unset | Backend-only credential |
| `OPENAI_MODEL` | `gpt-4.1-mini` | Model supporting Responses + Structured Outputs |
| `RETRIEVAL_TOP_K` | `3` | Maximum retrieved candidates |
| `MIN_SIMILARITY` | `0.12` | Minimum cosine similarity |
| `MIN_COVERAGE` | `0.85` | Minimum weighted coverage per clause |
| `PORT` | `8080` | Backend HTTP port |
| `SERVER_ADDRESS` | `127.0.0.1` | Backend bind address |

Other bounds and provider settings live in `src/main/resources/application.yml`. If you change `PORT`, update the Vite proxy target in `frontend/vite.config.ts` too.

## Build artifacts

`mvnw verify` generates `target/support-answer-1.0.0.jar` (use `sh ./mvnw verify` on macOS/Linux). Run it from the directory containing the intended `kb.json`, or set `KB_PATH`:

```sh
java -jar target/support-answer-1.0.0.jar
```

`npm run build` generates `frontend/dist/`. For local verification of that build, run `npm run preview` from `frontend/` while the backend is running; Vite previews it at <http://127.0.0.1:4173>. The JAR serves the API and Swagger; the Vue build is a separate artifact. Generated dependency caches, `target/`, and `frontend/dist/` are ignored and reproducible with the commands above.

## Source map

```text
kb.json                         External runtime input
src/main/java/org/example/
  config/                       Settings, SDK client, index and OpenAPI wiring
  controller/                   /ask, /api/kb, /api/status, input errors
  model/                        Public JSON records and internal evidence
  retrieval/                    Token normalization and immutable TF-IDF index
  service/                      File loader, passage selection, support gate, orchestration
  answering/                    Extractive/OpenAI composers and final validation
src/test/                       API, replacement-input, grounding and SDK tests
frontend/                       Vue + TypeScript interface
evaluation/                     Eight-case HTTP validation script
```
