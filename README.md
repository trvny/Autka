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
<i>❝“There are only two things wrong with C++:  The initial concept and the implementation.”— Bertrand Meyer❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [How Populist Middle Powers Are—and Are Not—Reshaping Global Politics](https://carnegieendowment.org/research/2026/08/how-populist-middle-powers-areand-are-notreshaping-global-politics)
- [Trump administration to impose 15% tariff in polysilicon probe meant to counter China](https://news.google.com/rss/articles/CBMirgFBVV95cUxON0ctWlNTcE9CNEpzbmE4cXRCdFVEU2k2dVpMNjc0M0xLQU1hSWR2MmgwZjdXenNzVnlSM0dGUWVmVlJzUGJ1b3kxV0NReVNqRFItdHlqbzcwR085cmJ5NFRBRlNWTXBRLXRRcmh1Uzl0Wmo2N2d3Wjd1MmpKNkN6Umo4bHlZdFhsbUZYWTVFeGhBUjVCZ3pyNy1EYkVFaWMzVC1ZSzRlQnVYS21LZWc?oc=5)
- [US Senate confirms Schwartz as CDC director](https://news.google.com/rss/articles/CBMiigFBVV95cUxPdUlSeHMyTFMzMzBSYXp5UHJJZ2dQTEZJYTJ5c1dtdWZHaXJ0WUNMRnVIRkpJY2c5dlRWcVdybmVPbFhJcHZ1XzBaZHRPWUJlY181eDItTEtraEY3d0V6dGJRVE9ReXFJcnktcGJEY25nRWtTbGFpRzQ5UXZXSkVVRWJZYTNrM21fQ0E?oc=5)
- [Etsy lays off 12% of workforce as part of restructuring plan](https://news.google.com/rss/articles/CBMiqgFBVV95cUxOYWh3SWlrVGlSZDBVdnBWajVZS3N1ejdBVVN6SlY2V3JIdVRQOVc2VGFtX1Y1MHNsblFON3pWVkhZbHU4WmtoQWZWNlZJQUZLMDlkMTEyNDBycEd1Y3BMMXRHUDNnSHZMNHdFY3FycS12R080bTZ5TEVfQmFzOE1ONzJ5WlpMS0trVFV6T3lpRFo2WVRiSUpOWW5hZDlZczgtdnhYeEVZRnBrQQ?oc=5)
- [ZKKM Chrzanów uruchamia nową linię i zwiększa liczbę połączeń - Przelom.pl](https://news.google.com/atom/articles/CBMirwFBVV95cUxQdDkxMHY5VUR3VW5RZmZLWGxPaXRjZ1Z4U2QxbDZNYS1uLWl6Q1oyUVRlUHEtQTh3QmI5VHFrUEtQNVYxZmo0dTIyT0NUWkExeEVQTmprQlBtZ1RBdm54c1Nfa1FiZG05OUQ0dkU0bTg5cFpvVEx6VTY0NWxBZWprMGtsd0dTYkRERmUwckZZOU1ad2JyWi1LdnJkX0tVT2tLRldWNTdJZXJ5UUZqWi1J?oc=5)
- [EXCLUSIVE: Iran threatens to hit Gulf states if US launches new strikes](https://news.google.com/rss/articles/CBMisAFBVV95cUxPajFnaXk0anVrT2IwaV9XMDBma0pMQU53ZF9LRUJiT01nWjJsdG5FZXhwUjNtZFp2Yy1NV3Q4dFRIeUxzV1pCWk9RMVVZNk1UbWxxMFdiNEdNTG5KSVBVWTRCaWFJSFNlT3RiMFF6RWtqN2pnRThTUU9GaHNfelhwV3RZTXdfMzJwUk1aVlFXZ0FBNk1qWFFmd2xIRWNVWVpmQl9DSkE2NnhMaXlJZ2FFeQ?oc=5)
<!--README_FEED:END-->
