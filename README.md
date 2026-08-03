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
  <img src="https://img.shields.io/badge/kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.4.10">
  <img src="https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white" alt="minSdk 26">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/trvny/autka" alt="Apache-2.0"></a>
  <a href="https://deepwiki.com/trvny/autka"><img src="https://deepwiki.com/badge.svg" alt="DeepWiki"></a>
</p>

An Android app that aggregates used-car offers from Polish, EU, and US-import
marketplaces into one searchable list, with landed-cost estimation for cars imported
from the USA.

> Formerly **CarGate**.

> [!IMPORTANT]
> **Current blocker / Obecna blokada**
>
> **EN:** Autka is currently limited less by code than by access to lawful, reliable
> listing data. Scraping marketplaces is not the direction of this project, while
> arranging a licensed feed or a business partnership with a major platform such as OLX
> is not something a small independent project can do overnight. Leads to legal data
> providers, API/feed access, relevant contacts, or concrete collaboration offers are
> very welcome.
>
> **PL:** Rozwój projektu Autka ogranicza dziś nie tyle kod, co dostęp do legalnych i
> wiarygodnych danych ogłoszeniowych. Scraping serwisów nie jest kierunkiem tego projektu,
> a dogadanie licencjonowanego feedu lub współpracy biznesowej z dużą platformą typu OLX
> nie jest dla małego, niezależnego projektu sprawą do załatwienia od ręki. Kontakty do
> legalnych dostawców danych, dostęp do API/feedów albo konkretne propozycje współpracy są
> bardzo mile widziane.

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

## License and third-party materials

Original source code and documentation are licensed under [Apache-2.0](LICENSE).
Marketplace data, names, trademarks, logos and linked media are not covered by that
license. See [THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md).

---
## Other stuff

[![feeds](https://github-stats-extended.vercel.app/api/pin?username=trvny&repo=trvny%2Ffeeds&theme=great-gatsby)](https://github.com/trvny/feeds) [![tvpi](https://github.com/trvny/.github/blob/main/assets/profile/pin_tvpi.svg)](https://github.com/trvny/tvpi)

## 💬 Cytat z szuflady

<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝The person I like most is the one who points out my defects. — Umar ibn Al-Khattāb (R.A)❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- ["To nie będzie bimbrownia". Inwestor stanowczo odpowiada mieszkańcom - Przelom.pl](https://news.google.com/atom/articles/CBMisgFBVV95cUxPeWJIRnpQaW4wUUhxV0lGSE0xdklPZ0hleGxZYVhlQUdvcHdzSzFJR2NjRHlsVmJWVFpRa1J3X0lER3BOR3JVSkRodl9QMzN2UHlleWtZRlRyT2p3UU9OYkZQNUtDX3ltMUZTc3VWY1ZxRTZNbU1mbFB1bXdnbDRORC1ZX2hsYzd1eHNYVW9Tb3pzRlYtaEp5WWZLM254V09OZDZrdEw4MWlyVFBZN3NtTjFR?oc=5)
- [FIFA has scrapped $20 billion World Cup sell-off plan, New York Post reports](https://news.google.com/rss/articles/CBMiuwFBVV95cUxNbXdUU2FsekNqTWc1TWdVd09xUXpTTXNScnVjczNNcGVzbERTWmVwOWNGMGlOd2J2dWQwem4yOU4yb0Y0VThodnEzcWJTRjRBZjI2RUhsNDdNXzNtMmVCa2tBNVp2U3lraTE2ZjFObG9WYThxaGJBWVpTSjE2YV9TTnAwNWVXT0d0MjVTcWJlRUs2bjN5bUJzVEU0bG5NNlZ0X1pwSUZPRTF3TjN2QjNWWDRGeWR6SzhTd1pn?oc=5)
- [US bars imports from 43 more companies over China's alleged forced labor involving Uyghurs](https://news.google.com/rss/articles/CBMiwgFBVV95cUxOSzRCdVNpdm4wZldHbEt1WTduUnNQTnh6N2didEJVLTQ5RjBYSGtQemRacGtrRFdjOUt0Ukh0X0lyLS03cDA2YVRWd3piM3V6N1VZZkprcU52QzBJWW10XzRvTnZQUnBwTURxbkJabXduaV82Q0wyd2xWQW5DNERZM2poRlAtM2xmV0tNSGt4LWc0RkVhSTRSQktmeWRfMjlrR0pGQW93Nm9OUnoteEU4b3RQSWxNN2NZaEwxbHdSYUVQdw?oc=5)
- [Exxon, Chevron warn of continued high fuel prices from Iran war](https://news.google.com/rss/articles/CBMiqgFBVV95cUxNMjdEbTVuZzlMdmNabW90dk04NEdXMUdBUFNzR2FpcDJYZE9RWnlQdXFNV1NtZkVWZ2J1M0dzTXhwVGJFYlNsUkp2d211bFB4eXRQQXk3RWJ5c1hBZ1BUYnF3VXNiSzFweUpEaktkOUsyLVBISHlZY2dneU5RTnkyeXNTejVUZlpJa3hURjFsMGZkSVUxVUw4QUg0N3pqZjg0UlZVdTNGSG9kZw?oc=5)
- [US, Israel planning to bombard energy-related targets in Iran, CBS reports](https://news.google.com/rss/articles/CBMivAFBVV95cUxQNXpvc21vbVJNVjc3NWg0ZnppblFCM3dmQXFVVTNhc1A1YkhoSTA4Q0tFZ0hQbFN0WUdtRlBza0J1dzBFenk3NW85U0ZWLXA0N1llLVFCNUdmRy1JU2NzOTE2bTFTdmlYUXdZNUphTE1uSE84ZXA0WjBYNkUtX2NBbWpUS08taFBRc0xNdGtoV0F6SHpfdU15dFB1WllYNnVwQkxFWThYelpLZnQyRml2Q0MwLVZ2blFDWl9raA?oc=5)
- [EXCLUSIVE: OpenAI finds evidence other AI agents escaped containment as it widens hacking probe](https://news.google.com/rss/articles/CBMivAFBVV95cUxQYTc2SUhrNmVER0NvNW9nZHBwbklFMlA0eTNSRFZETXpfSWpVYU1wOUhaMDRLUnRVSEhtRzByeEktX2FEX1ZzaThNMnpZMW9JdVZDZkVRTEQ4UjdlakFPVXZTUjZaM2JOUkhpN1BxeEU4bWJuSjNkUldBNXRVWDkyYWVXVUlKM0VEZU5wZklfX2FJRkQ0bmVybTIyY0xpbHd6aGtIVmZ3YU5ocGdsdFlNeXdOQlBXOUsyZnZtZA?oc=5)
<!--README_FEED:END-->
