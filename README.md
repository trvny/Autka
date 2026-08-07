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

# Autka

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

## License

Original source code and documentation are licensed under [Apache-2.0](LICENSE).
Marketplace data, names, trademarks, logos and linked media are not covered by that
license. See [THIRD_PARTY_NOTICES](docs/THIRD_PARTY_NOTICES.md).

---
## Other stuff

[![feeds](https://github.com/trvny/.github/blob/main/assets/profile/pin_feeds.svg)](https://github.com/trvny/feeds) [![tvpi](https://github.com/trvny/.github/blob/main/assets/profile/pin_tvpi.svg)](https://github.com/trvny/tvpi)

## 💬 Cytat z szuflady

<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝Conquer anger with non-anger. Conquer badness with goodness. Conquer meanness with generosity. Conquer dishonesty with truth. — Buddha❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [Confronting the Barriers to AI Diffusion in the U.S. Military](https://carnegieendowment.org/research/2026/08/confronting-the-barriers-to-ai-diffusion-in-the-us-military)
- [US judges allow Trump to end protections for migrants from South Sudan, Myanmar](https://news.google.com/rss/articles/CBMisAFBVV95cUxOS0R4Q05HNGkyM2Zzaml1b0lTTDJBeFpJWUx6YXZIOVM2RmJ1b0Nidk1qamJmQ2oyUFVVWW0wNWVCV0ZGRkx0c2gzdkh2c2F1TEZPR0lHRlUyNFJHdVhQYm1Ja2gwVjVCTkY5b2hHbWFtbFlBTlZDUTVFRDFERGEwSXhac3dXTHpRbFRxb1NYWHd6VWV6Qjl5bjFlZXVrMkZKOTdJWEZzcTRNSThsdEt1Vw?oc=5)
- [Spain's government announces immediate border controls with Italy in migration spat](https://news.google.com/rss/articles/CBMixwFBVV95cUxPTTcwY21xTGlrcmc5ZmE2cmRUMFpvSExKMTRoZWVubE9PbWpEbGl2dEo1U1BLNDViRDdpbVRmUUhvQmpzUWU4S3k4RnpfeFp0YVFQanRveFJpdkhtcUdlZFVGSTQ1bU5zRWNiQXdWX2ZILW5GeVMtRUJxbFQxTzR0dEN2VEYwandzR3BZb0MyZ0VjNVBXejlBU2UxUHg3YzNsNGdIbzVMaWd1UllZZDlGR2R1RkduWUNPdE9yNGFmVW5Ca19OQmlR?oc=5)
- [US official: We expect a deal soon between Iran and Oman on Strait of Hormuz](https://news.google.com/rss/articles/CBMiuAFBVV95cUxPMkZHc1FWRXNSWG5BdjNhZ0dqOEdNUFUtUl80NTNFYkFrM2RrLVBiSXlOOFdaek5hcEhIMl84OVJLZnVDMzNodFBScmliaFdNYUpaN21uQ3V3VmQ2V1p3MGZ0QUozdTJQb09EcTJ1VERWc3E0d1Z5dkNvalFEcEVSQzRQejkwZmdybVJTVDJPbk4wMS1hWFBOaXJKaTE4MUxHNS1STUFxTzN4S0ZDSmtZOXFhdUZaa0d1?oc=5)
- [EXCLUSIVE: Trump administration to back three mineral projects with $58 million in financing](https://news.google.com/rss/articles/CBMiuwFBVV95cUxPdVZodXZtRWQ1dzdvLWxpTi1jTFNKUlF4RnYxc2JSTnZDdXE3S0gtQ1R5MENuMFlUU1M2S1pMX3pMVFBqZGh6NjhlOUNybmd3MU5WR2RVOG91Wml5WGFNS0w1OVhSWDFiWmJsRzBoRzlJZHNnUG5LeEN3M3RaNW9mNkVvMkYzazRRZF85ek5aYk5kOExBNERwTlN5dGNVdjl4MnZGRXRCZnB0NGNJaHRVWERQU2gzd3pqRVpr?oc=5)
- [Przegląd AI: 7 sierpnia 2026](https://promptowy.com/przeglad-ai-2026-08-07/)
<!--README_FEED:END-->
