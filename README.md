<!-- markdownlint-disable MD041 MD033 -->
<p align="center">
  <img src="https://raw.githubusercontent.com/trvny/autka/main/fastlane/metadata/android/en-US/images/icon.png" width="128" alt="Autka icon">
</p>

<h1 align="center">Autka</h1>

<p align="center">
  Open-source Android project for car shopping across Poland, Europe and US imports — with a built-in USA → Poland landed-cost calculator.
</p>

<p align="center">
  <a href="https://github.com/trvny/autka/actions/workflows/android-ci.yml"><img src="https://github.com/trvny/autka/actions/workflows/android-ci.yml/badge.svg" alt="Android CI"></a>
  <a href="https://github.com/trvny/autka/actions/workflows/shared-ios-ci.yml"><img src="https://github.com/trvny/autka/actions/workflows/shared-ios-ci.yml/badge.svg" alt="Shared iOS CI"></a>
  <a href="https://github.com/trvny/autka/actions/workflows/backend-ci.yml"><img src="https://github.com/trvny/autka/actions/workflows/backend-ci.yml/badge.svg" alt="Backend CI"></a>
  <img src="https://img.shields.io/badge/Android-minSdk%2026-3DDC84?logo=android&logoColor=white" alt="minSdk 26">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/trvny/autka" alt="Apache-2.0"></a>
  <a href="https://deepwiki.com/trvny/autka"><img src="https://deepwiki.com/badge.svg" alt="DeepWiki"></a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/trvny/autka/main/fastlane/metadata/android/en-US/images/featureGraphic.png" width="720" alt="Autka feature graphic">
</p>
<!-- markdownlint-enable MD041 MD033 -->

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

## What Autka is

Buying a car across borders means jumping between marketplaces, currencies, filters,
auction sites and import calculators. Autka is an attempt to make that decision process
feel like one product instead of twelve browser tabs.

When a lawful listing feed is available, the backend can normalize it into one catalogue.
When a marketplace cannot be ingested, Autka keeps the user's search intent and hands the
search off to the original site through a deep-link instead of scraping it. For cars from
the USA, the same app can estimate what the vehicle may cost after shipping, duty, Polish
excise and VAT.

> Formerly **CarGate**.

## What already works

- **Search, filters and sorting** over the connected/cached catalogue, including make,
  price, year, mileage, fuel, transmission, region and source.
- **Marketplace hand-off** to original PL/EU/US search pages when Autka has no licensed
  feed for that source.
- **USA → Poland import calculator**, available both from a listing and as a standalone
  tool, with shipping, customs duty, excise and VAT breakdowns.
- **PLN / EUR / USD comparison** using NBP exchange rates, with explicit stale-rate
  fallback rather than silently pretending cached rates are current.
- **Map view**, **source-status diagnostics** and an import-service directory.
- **Offline-first results**: the last successfully cached catalogue remains usable when
  the network is unavailable.
- **No ads or analytics SDKs**. The Android manifest currently asks only for internet and
  network-state permissions.

Import figures are estimates, not customs quotes. Exact classification, valuation,
shipping, tax and relief rules can change the final amount.

## Looking for data and business partners

The most useful contribution to Autka right now is not another scraper. It is a lawful
way to make the catalogue real.

Potential fits include:

- marketplaces, aggregators or data vendors able to license search/listing data;
- dealers and importers willing to provide an authorised JSON/CSV/API inventory feed;
- US auction buyers, brokers and logistics partners who can provide realistic shipping
  and landed-cost inputs;
- connected-seller integrations where a business explicitly authorises access to its own
  inventory;
- affiliate or lead-generation partnerships that can fund the project without turning it
  into an ad farm.

A first integration does not need to be enormous. It does need enough structured data to
normalize each listing into Autka's offer model: stable source/id, title, make and model,
price/currency, fuel, transmission, region and original listing URL, plus clear update and
expiry semantics. Optional fields such as year, mileage, location and images make the
result substantially more useful. See [`docs/DATA_SOURCES.md`](docs/DATA_SOURCES.md) for
the full boundary and partner checklist.

## Current project state

The Android app and Cloudflare backend are functional, but Autka deliberately does not
fabricate production inventory. Sample offers are opt-in for debug/demo builds only. Until
a compliant live provider is connected, the internal production catalogue can be empty
and marketplace deep-links remain the main hand-off for unsupported sources.

That makes the project useful today as an import-cost/search companion, while the full
aggregator vision depends on data partnerships rather than reverse-engineering private
marketplace endpoints.

## Development

Android development requires JDK 17 and a recent Android Studio:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

The Worker lives in [`backend/`](backend/) and has its own setup and operations guide in
[`backend/README.md`](backend/README.md).

## Documentation

| Document | Purpose |
|---|---|
| [`docs/DATA_SOURCES.md`](docs/DATA_SOURCES.md) | Data policy, partner integration paths, deep-link boundary and source acceptance checklist |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Android/backend architecture, currency, import costs, map, caching and CI |
| [`docs/RELEASING.md`](docs/RELEASING.md) | Signing and release process |
| [`docs/TODO.md`](docs/TODO.md) | Remaining product blockers and engineering follow-ups |
| [`backend/README.md`](backend/README.md) | Backend API, ingestion and operations |
| [`docs/THIRD_PARTY_NOTICES`](docs/THIRD_PARTY_NOTICES) | Ownership and licensing boundary for third-party material |

## License

Original source code and documentation are licensed under [Apache-2.0](LICENSE).
Marketplace data, names, trademarks, logos and linked media are not covered by that
license. See [THIRD_PARTY_NOTICES](docs/THIRD_PARTY_NOTICES).

---
## Other stuff

[![feedseek](https://github.com/trvny/.github/blob/main/assets/profile/pin-feeds.svg)](https://github.com/trvny/feedseek) [![tvpi](https://github.com/trvny/.github/blob/main/assets/profile/pin-tvpi.svg)](https://github.com/trvny/tvpi)

## 💬 Cytat z szuflady

<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝Stay is a charming word in a friend's vocabulary. — Louisa May Alcott❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [Family says Massachusetts mother who killed children struggled with mental illness](https://www.reuters.com/legal/government/massachusetts-woman-searched-online-postpartum-psychosis-days-before-killing-2026-08-17/)
- [Incident with GitHub.com](https://www.githubstatus.com/incidents/zkxwbgr0cnmx)
- [Tylko jeden oferent na budowę parkingu za ponad 20 mln zł w Trzebini - Przelom.pl](https://news.google.com/atom/articles/CBMivAFBVV95cUxPNFM4eEg4d0g1UkVPRlVHZ2RSUjE4ckt6U19mQ296S3FmaUxPcW1TeTA0NnM1MVNiTGVDR0JGakl3QUxvLW0wOW8zZFBvY1p5TzcwRGp6REJpWVZVb3JRRlBmQjlhSHNwZ1lyTENNMFpvWFNYdHZZdUVOWm84Y194UzIzMm1rSkp2N3A3eE9FSzF3WVpXVEdqbFQ1TVc4VHNrTUpKOFFpd2xqMzhUUmZNZGxKdEVNSUtVczNsaw?oc=5)
- [EXCLUSIVE: Trump approval falls to 33%, lowest of his presidency, Reuters/Ipsos poll finds](https://www.reuters.com/world/us/trump-approval-falls-33-lowest-his-presidency-reutersipsos-poll-finds-2026-08-17/)
- [Susza hydrologiczna – woda, której nie widać - jaw.pl](https://news.google.com/atom/articles/CBMiYEFVX3lxTE5ITmJxdU1JWkpSQndSUWEybVNzQjNtaDRNRmtld0Ewc05QZmkxekFmQzBHM1dFcTc4cEV0azJBX0c1NThQbEtkT0ZBYm9mbnNJbVhMRGhBOWRLVTNwVWRrTg?oc=5)
- [How a soldier’s video diary from Ukraine’s front line became a Reuters documentary](https://www.reuters.com/world/ukraine-russia-war/how-soldiers-video-diary-ukraines-front-line-became-reuters-documentary-2026-08-17/)
<!--README_FEED:END-->
