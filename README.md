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
