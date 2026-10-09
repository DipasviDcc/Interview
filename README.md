# Evidence Support

A small Spring Boot + Vue support service that reads `kb.json`, retrieves evidence with TF-IDF, and either returns a cited answer or abstains. It runs without an API key. An optional OpenAI mode uses the official Java SDK and Responses API.

The Vue application includes a question form, answer and abstention states, expandable citations, retrieval details, a searchable **read-only knowledge-base viewer**, and a link to **Swagger UI**.

## 1. Source code

The backend source is in [src/main/java/org/example](src/main/java/org/example), and the Vue frontend is in [frontend/src](frontend/src). The repository layout is:

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

## 2. Setup instructions

### Prerequisites

- JDK 17 or later (tested with Java 17).
- Node.js 22.12 or later and npm for the Vue frontend and HTTP evaluation script.
- Internet access on the first build to download Maven/npm dependencies. Maven is bootstrapped by the checked-in wrapper; a separate Maven installation is unnecessary.
- An OpenAI API key only when using OpenAI mode. No secrets are needed for tests or the core service.

Pinned dependencies: Spring Boot 3.5.16, OpenAI Java 4.78.1, springdoc 2.8.17, Vue 3.5.43, Vite 8.3.4. Frontend transitive versions are recorded in `frontend/package-lock.json`.

### Install frontend dependencies

Clone this repository and open its root directory, which contains `pom.xml` and `kb.json`. Run backend commands from that root directory. The Maven Wrapper downloads Maven and backend dependencies on the first build.

Install the frontend dependencies once, before starting Vite:

```sh
cd frontend
npm ci
cd ..
```

If you are already in `frontend/`, run `npm ci` there and then return to the repository root for backend commands. On Windows, stop any running Vite dev/preview server with Ctrl+C before reinstalling dependencies; otherwise its native Rolldown module can cause `EPERM unlink`.

### Choose the answering mode

Set these variables in the same terminal that will run the backend. Environment changes take effect when the backend restarts.

**Offline mode — no API key required:**

PowerShell:

```powershell
$env:ANSWER_MODE = "extractive"
```

macOS / Linux:

```sh
export ANSWER_MODE=extractive
```

**OpenAI mode — set your API key:**

Replace `your-api-key` below with your own key, then start the backend using section 3. Keep the key in the backend environment; do not put it in Vue source code or commit it to Git.

**PowerShell:**

```powershell
$env:ANSWER_MODE = "openai"
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_MODEL = "gpt-4.1-mini"

```

**macOS / Linux:**

```sh
export ANSWER_MODE=openai
export OPENAI_API_KEY=your-api-key
export OPENAI_MODEL=gpt-4.1-mini

```

Use a model available to your API account that supports Responses and Structured Outputs. The model name is configurable. `.env.example` documents the settings; `.env` files are **not automatically loaded** by Java or these commands.

OpenAI receives only the current question and approved retrieved excerpts. Requests use `store=false`, a 20-second per-request timeout, at most one SDK retry, and a 1,200-token output limit. There is no conversation memory or web search.

To make citation grounding directly checkable, **both modes return verbatim excerpts**. In OpenAI mode the model selects complete approved excerpts into a structured claim list. It does not freely paraphrase the facts. The backend rejects altered quotations, missing subquestions, duplicate claims, and unknown document IDs. This deliberately trades conversational wording for verifiable evidence.

If the key is missing or the provider fails, the service uses the checked extractive composer. A model refusal or a well-formed but invalid claim list causes abstention. Parsing failures and incomplete provider responses use the same validated offline fallback as provider errors. Logs record the exception type without logging API keys, question text, or provider response bodies. `/api/status` reports configuration availability, not a live credential validity check.

Official references: [Java SDK](https://developers.openai.com/api/reference/java), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs), [configured default model](https://developers.openai.com/api/docs/models/gpt-4.1-mini).

## 3. Run command

Run backend commands from the repository root, where `kb.json` lives.

**Windows / PowerShell — terminal 1:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Windows / PowerShell — terminal 2:**

```powershell
cd frontend
npm run dev
```

**macOS / Linux — terminal 1:**

```sh
sh ./mvnw spring-boot:run
```

**macOS / Linux — terminal 2:**

```sh
cd frontend
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

Stop the Vite dev/preview server before running `npm ci` again on Windows. Vite holds the native Rolldown module open, which can cause `EPERM unlink` during dependency installation. Use Ctrl+C in the terminal running Vite, then retry `npm ci`.

## 4. Validation command

Run automated backend validation from the repository root. The backend server does not need to be running, and these tests do not require an OpenAI key.

**Windows / PowerShell:**

```powershell
.\mvnw.cmd verify
```

**macOS / Linux:**

```sh
sh ./mvnw verify
```

Expected result: all tests pass with zero failures/errors, and Maven creates `target/support-answer-1.0.0.jar`. Validation loads the repository `kb.json`, checks the required JSON keys, tests cited answers and unsupported-question abstention, and verifies replacement KB files. Sample-specific tests use their own fixture so a valid replacement KB does not break those expectations.

**Frontend type checking and production build:**

Run from `frontend/` after the setup step has installed dependencies:

```sh
npm run build
```

Expected result: TypeScript checking passes and Vite creates `frontend/dist/`.

**Live HTTP evaluation:**

Start the backend as described in section 3, keep it running, then run this from a separate terminal at the repository root:

```sh
node evaluation/evaluate.mjs
```

For the bundled sample KB, expect all eight decisions to match, a citation on every answered question, valid source snippets and JSON fields, and zero unsupported questions answered. The script exits nonzero if a check fails. See section 6 for test details, custom evaluation cases, and manual testing.

## 5. Sample kb.json

The repository includes the runtime sample file [kb.json](kb.json), containing six FAQ articles about password reset, two-factor authentication, withdrawal review, account closure, address changes, and unsupported request types.

Each entry uses this schema. For example:

```json
[
  {
    "id": "doc_1",
    "title": "Password reset",
    "text": "Users can reset their password from the login page by clicking 'Forgot password'. A reset link is sent to the registered email address. The link expires in 30 minutes. Support agents cannot manually view or send existing passwords."
  }
]
```

### Use a replacement KB

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

## 6. Tests and evaluation scripts

### Automated test files

Tests live in `src/test/java/org/example/`:

| File | What it checks |
|---|---|
| `AnswerServiceTest.java` | Supported and unsupported questions, exact evidence, negation, timing caveats, role/action and time-unit mismatches, multi-part questions, invalid model claims, provider failure, conflicts, and KB instructions |
| `KnowledgeBaseLoaderTest.java` | Reading the real input file, replacement/reloaded KB content, malformed/missing files, duplicate IDs, and an empty KB |
| `SupportApiTest.java` | Required response keys, HTTP status codes, invalid inputs, read-only KB access, and Swagger/OpenAPI endpoints |
| `OpenAiAnswerGeneratorTest.java` | The official SDK's structured Responses request against a local HTTP stub, invalid citations, and missing-key fallback |
| `OpenAiFallbackContextTest.java` | Starting Spring Boot in OpenAI mode without a key and retaining the offline retrieval/abstention flow |

Run every test with the validation command in section 4. To run a single test class, for example:

**PowerShell, repository root:**

```powershell
.\mvnw.cmd "-Dtest=AnswerServiceTest" test
```

**macOS / Linux, repository root:**

```sh
sh ./mvnw -Dtest=AnswerServiceTest test
```

Test reports are generated in `target/surefire-reports/`. The OpenAI tests use a local HTTP stub and the real official SDK; automated tests do not make paid API calls.

### Evaluation script

- [evaluation/questions.json](evaluation/questions.json): eight questions with expected `answer` or `abstain` decisions for the sample KB.
- [evaluation/evaluate.mjs](evaluation/evaluate.mjs): sends questions to the running backend and validates decisions, citations, and JSON structure.

```sh
node evaluation/evaluate.mjs
```

The JSON report includes `decisionAccuracy`, `answeredCitationPresenceRate`, `citationSourceMatchRate`, `jsonContractRate`, and `unsupportedAnsweredCount`. Citation/source matching checks exact original text; it is not an independent measure of semantic relevance.

For a different backend address or replacement KB, create a cases file such as:

```json
[
  { "question": "A question supported by your replacement KB", "expected": "answer" },
  { "question": "A question absent from your replacement KB", "expected": "abstain" }
]
```

Run it from the repository root:

```sh
node evaluation/evaluate.mjs http://127.0.0.1:8080 path/to/cases.json
```

The bundled eight questions target the sample KB. Use matching custom cases for unrelated replacement content. The script fetches the currently loaded KB to verify citation IDs, titles, and exact snippets.

### Manual API testing

Keep the backend running. With the bundled `kb.json`, use these checks:

| Question | Expected result |
|---|---|
| `How do I reset my password?` | `decision: answer`, at least one citation to the password-reset article |
| `How long is the password reset link valid?` | `decision: answer`, source text stating 30 minutes |
| `Can you guarantee my withdrawal will finish in 2 hours?` | `decision: abstain`, empty citations, a human-support fallback |
| `Can support change my email address for me?` | `decision: abstain`, empty citations |

**PowerShell — answerable question:**

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/ask -ContentType application/json -Body '{"question":"How do I reset my password?"}' | ConvertTo-Json -Depth 6
```

**PowerShell — unsupported question:**

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/ask -ContentType application/json -Body '{"question":"Can support change my email address for me?"}' | ConvertTo-Json -Depth 6
```

**macOS / Linux — answerable question:**

```sh
curl -s http://127.0.0.1:8080/ask -H 'Content-Type: application/json' \
  -d '{"question":"How do I reset my password?"}'
```

**macOS / Linux — unsupported question:**

```sh
curl -s http://127.0.0.1:8080/ask -H 'Content-Type: application/json' \
  -d '{"question":"Can support change my email address for me?"}'
```

For every answered question, check that each cited snippet appears in the corresponding loaded KB article and supports the answer. For abstentions, check for an empty citation list and a clear suggestion to contact human support.

### Manual frontend and Swagger testing

1. Start the backend and frontend using section 3, then open <http://127.0.0.1:5173>.
2. Submit an answerable question from the table above. Confirm the answer, supported status, source title, and expandable citation snippet.
3. Submit an unsupported question. Confirm the insufficient-information state and human-support message.
4. Open **Knowledge base**, search for `withdrawal`, inspect the original text, and use **Ask about this topic** to prefill a question.
5. Open **API reference**, or visit <http://127.0.0.1:8080/swagger-ui/index.html>. Expand `POST /ask`, choose **Try it out**, supply a question, and execute it. Check the returned JSON.

### Optional live OpenAI smoke test

Stop the backend, set `ANSWER_MODE`, `OPENAI_API_KEY`, and `OPENAI_MODEL` using section 2, and restart it using section 3. Repeat the answerable and unsupported API/UI checks above.

Inspect <http://127.0.0.1:8080/api/status> for `answerMode: "openai"` and `openaiAvailable: true`. These fields confirm configuration only; they do not prove the key is valid or that a provider call succeeded. Watch backend logs for an `Answer provider unavailable` warning, which means the checked offline fallback was used. Unsupported questions should abstain before calling the provider. A real API smoke test can incur usage charges; all automated tests remain local.

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
