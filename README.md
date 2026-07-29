<p align="center">
  <img src="https://raw.githubusercontent.com/trvny/autka/main/fastlane/metadata/android/en-US/images/icon.png" width="96" alt="Autka icon">
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

[![feeds](https://github-stats-extended.vercel.app/api/pin?username=trvny&repo=trvny%2Ffeeds&theme=great-gatsby)](https://github.com/trvny/feeds) [![tvpi](https://github-stats-extended.vercel.app/api/pin?username=trvny&repo=trvny%2Ftvpi&theme=yeblu)](https://github.com/trvny/tvpi)

## 💬 Cytat z szuflady

<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝All know the way; few actually walk it. — Bodhidharma❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [Marka Cashify od lipca funkcjonuje jako kantor kryptowalut online w oparciu o przepisy MiCA](https://pap-mediaroom.pl/biznes-i-finanse/marka-cashify-od-lipca-funkcjonuje-jako-kantor-kryptowalut-online-w-oparciu-o)
- [Erste Letnie Brzmienia 2026 ruszają już dzisiaj. Kraków otwiera letnią trasę przez pięć miast](https://pap-mediaroom.pl/biznes-i-finanse/erste-letnie-brzmienia-2026-ruszaja-juz-dzisiaj-krakow-otwiera-letnia-trase-przez)
- [Humanoid pozyskuje 152 mln USD przy wycenie na kwotę 1,35 mld USD po przeprowadzeniu rundy finansowania, stając się pierwszym europejskim jednorożcem wyspecjalizowanym w robotach…](https://pap-mediaroom.pl/biznes-i-finanse/humanoid-pozyskuje-152-mln-usd-przy-wycenie-na-kwote-135-mld-usd-po)
- [Fresha przyspiesza ekspansję w Europie, otwierając nowe biuro w Warszawie i powołując Macieja Walczewskiego na stanowisko dyrektora generalnego na Europę Wschodnią](https://pap-mediaroom.pl/biznes-i-finanse/fresha-przyspiesza-ekspansje-w-europie-otwierajac-nowe-biuro-w-warszawie-i)
- [Mikropoświadczenia - nowa waluta umiejętności](https://pap-mediaroom.pl/polityka-i-spoleczenstwo/mikroposwiadczenia-nowa-waluta-umiejetnosci)
<!--README_FEED:END-->
