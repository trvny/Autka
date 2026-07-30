<p align="center">
  <img src="https://raw.githubusercontent.com/trvny/autka/main/fastlane/metadata/android/en-US/images/icon.png" width="128" alt="Autka icon">
</p>

<h1 align="center">Autka</h1>

<p align="center">
  Used-car aggregator for Poland, the EU, and US imports — with landed-cost estimation built in.
</p>

<p align="center">
  <a href="https://github.com/trvny/autka/actions/workflows/android-ci.yml"><img src="https://github.com/trvny/autka/actions/workflows/android-ci.yml/badge.svg" alt="Android CI"></a>
  <a href="https://github.com/trvny/autka/actions/workflows/backend-ci.yml"><img src="https://github.com/trvny/autka/actions/workflows/backend-ci.yml/badge.svg" alt="Backend CI"></a>
  <img src="https://img.shields.io/badge/kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.4.0">
  <img src="https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white" alt="minSdk 26">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="MIT License"></a>
</p>

An Android app that aggregates used-car offers from Polish, EU, and US-import
marketplaces into one searchable list, with landed-cost estimation for cars imported
from the USA.

> Formerly **CarGate**.

## Status

Runnable scaffold. Search → filter → list → detail → import-cost breakdown works
end-to-end. Debug builds can use opt-in sample data; release builds and the production
Worker keep mock offers disabled. Live marketplace data requires a licensed, partner, or
seller-provided feed. Sources without one remain deep-links into their own search pages.

## Repository layout

This is a monorepo:

```
/            Android app (Autka) — Kotlin, Compose, root Gradle project
/backend     Cloudflare Workers backend — TypeScript, D1, R2
/docs        Architecture, sourcing, releasing, open items
```

## Build & run

Requires Android Studio (Ladybug or newer) and JDK 17.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or open the folder in Android Studio and hit Run. First sync downloads dependencies.

## Docs

| | |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | App layers, currency, import cost, map, de-dup, localization, versions, CI/CD |
| [`docs/INTEGRATION.md`](docs/INTEGRATION.md) | Compliant feeds vs. deep-links, and why Autka doesn't scrape |
| [`docs/SOURCES.md`](docs/SOURCES.md) | Vetted API/feed candidates and recommended acquisition order |
| [`docs/RELEASING.md`](docs/RELEASING.md) | Signing, Google Play and F-Droid release process |
| [`docs/TODO.md`](docs/TODO.md) | Remaining blockers and verification work |
| [`backend/README.md`](backend/README.md) | Backend API, ingestion and operations |

## Other stuff

[![feeds](https://github-stats-extended.vercel.app/api/pin?username=trvny&repo=trvny%2Ffeeds&theme=great-gatsby)](https://github.com/trvny/feeds)

## 💬 Cytat z szuflady

<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝“Tell me and I forget.  Teach me and I remember.  Involve me and I learn.”— Benjamin Franklin❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [The unauthorized tool call problem](https://www.answer.ai/posts/2026-01-20-toolcalling.html)
- [Incident with Copilot AI Model Providers](https://www.githubstatus.com/incidents/dsrfymph7my0)
- [Starbucks raises annual forecasts again as turnaround takes root](https://news.google.com/rss/articles/CBMipAFBVV95cUxNVkd3ZTVtSjBXeUllV1FMUnBlenlmYzNSbFk5bnpRUldTeXhFT0F5RnpVYk9yTDJDaVZ6QnNkNE52VTgtYmZ4WDRuSDc1MkxYMUI0V052SVA1SmwzbkhsTTNqLXBDamJMaTdBamxKNmR5TlhTcnExYnhFRHFPQVExSl9Rc0QzcGhZa2VtcS1Nc0VnYnZjMW9lZFNpRXFyb19nLTdKaw?oc=5)
- [Wisconsin judge says voters who have returned absentee ballot for state primary cannot get a new one](https://abcnews.com/US/wireStory/wisconsin-judge-voters-returned-absentee-ballot-state-primary-135203804)
- [Judge weighs bid to block or reverse transfers of transgender inmates into a segregated prison unit](https://abcnews.com/US/wireStory/judge-weighs-bid-block-reverse-transfers-transgender-inmates-135205583)
- [A Caribbean court rules that a US extradition process against Guyana's opposition leader can resume](https://abcnews.com/US/wireStory/caribbean-court-rules-us-extradition-process-guyanas-opposition-135205694)
<!--README_FEED:END-->
