# AGENTS.md

Rules for **any** agent working in `trvny/autka`. Tree-level rules — never run git at the
`~/git` root, fetch before touching a clone, the `trvny` account, the merge policy — live in
`~/git/AGENTS.md` and are not repeated here.

This file is the primary home for the invariants below. They are enforced by no linter, no
test, and no CI check; breaking one produces a change that builds green and is wrong.

## What this is

A used-car aggregator in two halves that must be read together:

- **`/app`** — Kotlin/Compose Android app, package `com.autka`
- **`/backend`** — Cloudflare Worker, TypeScript + D1 + R2

Aggregation happens **server-side**. The app binds a single `BackendCarOfferSource` (plus
`MockCarOfferSource`); per-marketplace adapters live in `backend/src/ingest/sources/` and are
registered in `ALL_SOURCES` in `backend/src/ingest/runner.ts`. Credentials and feeds never
reach the device.

**Most real bugs live at the seam between the two halves.** Reviewing one side alone misses them.

## Load-bearing invariants

**1. Offline-first; the cache is the source of truth.**
App: Room is authoritative. `OfflineFirstCarOfferRepository` exposes data as a `Flow` off the
DAO and refreshes the cache separately — the UI must never read the network directly. Backend:
D1 is the served store; the API reads D1, ingestion writes it. A ViewModel or screen consuming
a network result that did not pass through the cache is a defect.

**2. Source isolation.**
`runIngestion` runs each source under its own try/catch, so one failing feed cannot reject the
whole `Promise.all` or stop the others, and every run records an `ingest_runs` row. A bare
`Promise.all(sources.map(s => s.fetch()))` with no per-source guard is a defect, as is a
`fetch` that can throw out of the runner.

**3. CarOffer parity — the most common seam bug.**
`CarOffer` in `backend/src/lib/types.ts` must mirror `com/autka/core/model/CarOffer.kt`
field for field, and the enums (`Region`, `FuelType`, `Transmission`, `Currency`) must match
exactly on both sides. A field added on one side only means the app silently drops it or fails
to parse. The DTO and parsing in `data/remote/backend/BackendCarOfferSource.kt`, the Room
entity and its mapper are part of this chain.

**4. No scraping; credentials stay server-side.**
Sources must not scrape ToS-protected sites (Otomoto, OLX, Facebook). Without a compliant feed
they stay disabled as stubs (`isEnabled: () => false`, see `backend/src/ingest/sources/stubs.ts`).
Feed keys live in Worker `env` secrets — never committed, never a `buildConfigField`, never sent
to the device.

**5. Currency: convert through PLN before comparing.**
Offers arrive in PLN, EUR and USD. Any filter, sort or comparison across offers converts to a
common currency through PLN, using the cached `ExchangeRates` snapshot, first. Comparing raw
`amount` values across mixed currencies ranks them wrong. The snapshot is cached in DataStore;
decode it defensively.

**6. `core/` stays pure Kotlin.**
`core/model` and `core/util` carry **no Android imports** — no `android.*`, no `androidx.*`, no
Compose, no `Context`. Value objects, enums and the cost/exchange math live there so they stay
JVM-unit-testable.

**7. Persistent state is DataStore, not SharedPreferences.**
User settings and the rate snapshot use Preferences DataStore. Reuse the single app-wide
`DataStore<Preferences>` from `di/SettingsModule.kt` and namespace your keys rather than
creating a second one. Keys go in a `companion`; decode with `runCatching {}.getOrNull()`.

**8. Unidirectional data flow.**
Events down (`onAction`), state up (`StateFlow<UiState>` as a sealed interface). UI collects
with `collectAsStateWithLifecycle()`, not `collectAsState()`. Screen-scoped state uses
`SharingStarted.WhileSubscribed(5_000)`. The stateless `Screen` holds no ViewModel or Hilt
references.

**9. String parity.**
User-facing strings live in `res/values/strings.xml` with a `res/values-pl/` translation — the
app targets PL. No hardcoded user-facing strings, and no key present in one locale but missing
in the other.

**10. Bounded backend writes.**
The debug `ingest_runs` table is pruned to `INGEST_RUNS_KEEP`. Image caching (`images.ts`) is
best-effort and never throws. Unbounded writes on the request or cron path, and image failures
that can abort an ingest, are defects.

**11. Gradle hygiene.**
Versions come only from `gradle/libs.versions.toml`, referenced as `libs.*` — never hardcoded
in module files. The repo is on **AGP 9**, so Kotlin compiler options belong in the top-level
`kotlin { compilerOptions { … } }` block; `android.kotlinOptions` was removed and must not
come back. Maps use osmdroid, so no Google Maps key is needed.

## Working here

Keep an adapter file and its `ALL_SOURCES` registration in **one commit** — a registered source
that does not exist, or an adapter nothing calls, is worse than neither.

**Do not claim the project compiles.** Build, lint and test signal comes from CI —
`android-ci.yml` (`lintDebug assembleDebug testDebugUnitTest`) and `backend-ci.yml` — so report
the commit SHA and the run conclusion instead. Type-level reasoning about `backend/` has to be
done by reading the code, so read more of it than feels necessary.

There is no secret scanner in this repo. Grep changed files for key-shaped strings yourself
before pushing, matching on the **shape of the value** rather than the parameter name, and stop
if anything looks like a credential.
