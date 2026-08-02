# AGENTS.md

## Scope

These instructions apply to the whole `trvny/Autka` repository.

## Project invariants

Autka is an Android app plus a Cloudflare Workers backend. Read `README.md`,
`docs/ARCHITECTURE.md`, and `docs/INTEGRATION.md` before changing source or
ingestion behavior.

The current blocker is lawful, reliable listing data. Do not add scraping,
circumvent marketplace controls, or present an unofficial endpoint as a stable
licensed source. Production and release builds must not silently fall back to
mock offers; sources without an approved feed remain deep-links.

## Before changing anything

- Check current `main`, open pull requests, and recent changes.
- Determine whether the task belongs to the Android app, `backend/`, or both.
- Check `docs/SOURCES.md` and existing source abstractions before adding another
  provider or duplicate integration path.
- Treat pricing, currency, import-cost, and deduplication changes as
  correctness-sensitive business logic.

## Change rules

- Preserve the boundary between debug sample data and production data.
- Keep credentials, partner feed samples, seller data, D1 contents, and private
  marketplace responses out of the repository.
- Do not deploy the Worker, apply remote D1 migrations, publish an app, or alter
  signing configuration unless the task explicitly requests it.
- Keep Android and backend changes independently understandable; avoid broad
  cross-project refactors without a clear need.
- Update the relevant architecture or sourcing document when a durable project
  assumption changes.

## Validation

For Android changes use the CI command:

```bash
./gradlew --no-daemon --stacktrace lintDebug assembleDebug testDebugUnitTest
```

For backend changes use:

```bash
cd backend
npm ci
npx wrangler types
npm run typecheck
npm test
```

Do not run remote migrations or deployment as validation. Report anything not
run and distinguish sample-data tests from live-provider verification.

## GitHub workflow

Keep one logical change per pull request. Truly trivial low-risk edits may go
directly to `main`. Treat Codex review as advisory only; do not ask it to
implement, commit, or push. Prefer squash merge after relevant checks pass on
the final head commit.
