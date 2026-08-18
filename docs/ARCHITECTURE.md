# Architecture

This document describes how the current multiplatform app and backend fit together. Data-source
licensing, partner onboarding and the deep-link boundary live in
[`DATA_SOURCES.md`](DATA_SOURCES.md).

## App layers

The app is migrating incrementally to Kotlin Multiplatform. `shared` owns platform-neutral
models and contracts used by Android and future iOS code; `app` remains the Android host and
owns Android-specific UI, persistence, networking implementations and DI.

```text
shared/core/model        Normalized domain models and pure calculation/value objects
shared/data/remote       Source identifiers and CarOfferSource contract
shared/data/repository   CarOfferRepository/SourceInfo contract
app/core/util            Small Android-side utility/result helpers
app/data/local           Room database, DAO, entities and mappers; local offer cache is the Android source of truth
app/data/remote          Backend, NBP and official NHTSA vPIC API implementations plus debug MockCarOfferSource
app/data/repository      Android repository implementations for cached offers, rates, source health and VIN
app/data/imports         Import-service catalogue and backend/default fallback
app/data/settings        App-wide Preferences DataStore settings and lightweight saved-search snapshots
app/feature/listings     Search, filters and result list
app/feature/detail       Cache-backed offer detail and inline US import estimate
app/feature/importcalc   Standalone USA -> Poland import calculator
app/feature/vin          Provider-independent VIN decoding through NHTSA vPIC
app/feature/map          Offer locations and map interaction
app/feature/sourcehealth Public-safe backend source diagnostics
app/feature/external     Marketplace deep-links and import-service UI
app/di                   Hilt modules and source/repository bindings
app/ui                   Theme, navigation host and shared Android components
```

Android consumes one backend catalogue source, plus the optional debug mock source.
Per-marketplace aggregation happens server-side: adding a real marketplace means adding a
backend ingestion adapter, not another Android `CarOfferSource`.

Anything that produces catalogue `CarOffer` data goes through the backend. Marketplace
hand-offs that only produce URLs stay in `feature/external`. VIN decoding is also kept
outside the catalogue model: it is an explicit user-requested lookup against the official
NHTSA vPIC API and returns a separate `VinDecodeResult`. See
[`DATA_SOURCES.md`](DATA_SOURCES.md) for the maintained catalogue policy and partner
checklist.

## Backend boundary

Android calls the backend for the catalogue (`GET /offers`), import-service directory and
public-safe `GET /sources` health metadata. Offer details in the Android UI are not fetched
through the backend's single-offer endpoint: `OfferDetailViewModel` observes the selected
offer from the Room-backed `CarOfferRepository`, keeping detail consistent with the cached
catalogue. The backend still exposes `GET /offers/:id` as an API endpoint, but the current
Android `BackendApi` does not consume it.

The offer model in `backend/src/lib/types.ts` mirrors the shared `CarOffer` model and should
change in lockstep. `/sources` exposes enabled state, offer count and latest completed-ingest
metadata without sending raw provider errors to the device. Android deliberately does not
invent its own provider-specific freshness thresholds; a future stale/degraded state should
be driven by explicit backend cadence/failure metadata.

The Android backend URL is a `BACKEND_BASE_URL` build config field in
`app/build.gradle.kts`. Debug points at emulator loopback for local `wrangler dev`;
release points at the deployed Worker. Offer images may be cached in R2 and returned as
backend `/images/...` URLs.

For Worker endpoints, ingestion behavior and local deployment, see
[`backend/README.md`](../backend/README.md).

## Offer cache and failure isolation

Room is the Android catalogue source of truth. Successful refreshes **upsert** returned
offers into the local cache rather than treating every response as an authoritative
replacement. Only a successful full-catalogue refresh from every active transport is
allowed to run the global age-based cleanup, which removes cache rows older than seven
days; filtered refreshes do not expire unrelated cached offers. Disabled transport rows
are removed explicitly. A failed remote source does not wipe previously cached offers, and
source failures are isolated so healthy sources can still update the cache.

Backend ingestion has its own, separate snapshot semantics: a failed complete-snapshot
fetch does not delete the previous successful snapshot, while a successful snapshot may
expire source rows that disappeared from that authoritative feed. Every enabled backend
adapter must explicitly declare snapshot vs delta semantics before its cleanup behavior can
be trusted.

## US import cost

`shared/src/commonMain/kotlin/com/autka/core/model/ImportCostEstimate.kt` estimates landed
cost into Poland using shipping, EU customs duty, Polish excise and VAT. Excise is
drivetrain- and engine-capacity-aware; EV and hydrogen paths are modeled separately.

The calculator is available both from offer details and as a standalone tool. Both paths
reuse the same calculation logic, defaults and localized numeric parsing. Missing engine
capacity for a non-exempt drivetrain uses the deliberately conservative fallback described
in the UI rather than silently assuming a lower excise rate.

Duty, VAT edge cases, customs classification/origin relief and shipping defaults remain
indicative inputs. The result is an estimate, not a customs quote.

## VIN decoding

The standalone VIN helper accepts a full 17-character VIN and performs a user-triggered
lookup against NHTSA's public vPIC `DecodeVinValues` endpoint. Input is normalized locally;
letters I, O and Q are rejected before any request is made. Requests are never fired while
the user is merely typing.

Decoded VIN data stays separate from `CarOffer` and is not persisted as history. The UI
shows only fields returned by vPIC and keeps NHTSA decoder warnings visible alongside any
partial result. A missing decoded field is treated as missing NHTSA data, not proof that the
vehicle lacks that feature.

## Currency and local preferences

Offers may arrive in PLN, EUR or USD. `ExchangeRates` converts through a PLN base so price
filtering and sorting can compare mixed-currency results in the user's selected display
currency.

`ExchangeRateRepository` is offline-first. It can seed from persisted or built-in rates and
refreshes from the NBP public API. Cached/fallback rates remain marked stale until a fresh
NBP request succeeds. Preferences DataStore persists the selected display currency plus
small local saved-search snapshots. A saved search includes the complete `SearchFilter`
and its display currency so numeric price bounds retain their meaning when restored. These
snapshots remain separate from the Room offer cache and contain no marketplace listing data.

Large live catalogues will eventually need server-side normalized prices and cursor
pagination; until then Android requests one atomic complete set and performs price
conversion/filtering locally. That future work is tracked in [`TODO.md`](TODO.md).

## Map

Offers carry optional latitude/longitude. The map screen uses osmdroid with OpenStreetMap
tiles and opens the corresponding offer when a marker is selected. The map remains a
direct action in the listings toolbar.

Real ingestion adapters are responsible for supplying or deriving usable coordinates.
Sample/debug data includes coordinates for development.

## De-duplication

The same vehicle may appear through more than one source. The backend computes a heuristic
`dedup_key`, collapses matching offers in normal `/offers` responses and annotates the
result with listing/source counts. `?dedup=false` returns raw rows for diagnostics.

## Source-health diagnostics

The source-status screen reads `GET /sources` and shows public-safe state such as enabled
status, offer count and the latest completed ingest. The singleton repository keeps the
last successful response in memory for up to five minutes. A new screen instance renders
that snapshot immediately while starting a real network refresh, and manual refresh always
hits the backend. If a refresh fails, the visible snapshot is preserved alongside the error
state.

The cache is deliberately process-local, time-bounded and never persisted as source-health
history. Explicit backend health-availability/cadence fields remain possible follow-ups in
[`TODO.md`](TODO.md).

## Localization

UI resources live under `res/values/` with Polish mirrors under `res/values-pl/`.
Feature-specific XML files are preferred over one giant strings file when they improve
ownership, provided resource names remain unique and both locales stay in sync.

Numeric input uses locale-aware parsing; formatting that is visible in a localized screen
should use the active UI locale rather than assuming the system locale.

## Toolchain and dependency versions

Do not duplicate exact dependency versions in documentation. The maintained sources of
truth are:

- `gradle/libs.versions.toml` for Android/Kotlin libraries and plugins;
- `shared/build.gradle.kts` for shared Android/iOS targets and common dependencies;
- `app/build.gradle.kts` for Android SDK levels, app version and build-type configuration;
- `backend/package.json` for Worker dependencies.

Dependabot and CI keep these moving frequently, so hard-coded version tables in docs become
stale faster than they become useful.

## CI/CD

`.github/workflows/android-ci.yml` runs Android lint, debug assembly and unit tests plus the
shared Android-host test suite. The unit suites cover import calculations and parsing, VIN
input/response handling, listings behavior, repository failure isolation, exchange-rate
paths and source-health refresh semantics.

For pull requests, `.github/workflows/backend-ci.yml` regenerates Wrangler configuration
types, runs TypeScript checking and executes backend tests. On pushes to `main`, the same
workflow additionally applies remote D1 migrations and deploys the Worker.

`.github/workflows/release.yml` validates release signing inputs, builds signed APK/AAB
artifacts and creates a GitHub release for a `v*` tag. See
[`RELEASING.md`](RELEASING.md).

Dependabot covers Gradle/Kotlin, backend packages and GitHub Actions. Dependency submission
feeds the GitHub dependency graph for alerts.
