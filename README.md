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
<i>❝“The goal is to turn data into information, and information into insight.”— Carly Fiorina❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Mininewsy

<!--README_FEED:START-->
- [To najbardziej opłacalny smartfon z Androidem. Nowa wersja wkrótce w sklepach: co o niej wiadomo?](https://antyweb.pl/to-najbardziej-oplacalny-smartfon-z-androidem-nowa-wersja-wkrotce-w-sklepach-co-o-niej-wiadomo)
- [Kto dyrektorem Oświęcimskiego Centrum Kultury? - Beskidzka24.pl](https://news.google.com/atom/articles/CBMieEFVX3lxTE9VeWRfZ1lwa1lGY0RDTlplQ0JEWXg3cHN5bzFValdhQ1dxeXg3MkRhX1hlUWxlbkhQVnI2RFhvZ2hFTzRoVENKbWZkdDF1TDYyLXpicTYwTHVHUFg3cUd6dENaUFVmbjVxRVhsQWVNRHpxMV9keDVlYg?oc=5)
- [GPT 5.6 Luna i Terra będą 5 razy tańsze. Niespodzianka od OpenAI](https://antyweb.pl/gpt-56-nowe-nizsze-ceny-openai)
- [Google Pixel 11 zaskakuje. Ta funkcja wam się spodoba](https://antyweb.pl/google-pixel-11-ze-swietna-nowoscia-funkcja-ktora-pokochacie)
- ["Gorące elektrony" pozwolą nam tworzyć cuda z metalu. Ta metoda jest genialna](https://antyweb.pl/gorace-elektrony-metale-badania)
- [Myślał, że ratuje swoje pieniądze. W rzeczywistości przelał je oszustom - Przelom.pl](https://news.google.com/atom/articles/CBMiuAFBVV95cUxNcTVXZFFUbFBMVllMWTdneER6SWU3YUtjTkpUNHBjcHllQ3J4cU9xNmJrSHZUbzZ5aTVVM0VSd0VJYldpVTNHb0w5RGVEZ2VwQU5yS21mVnJkWlZPUlgySG1XdTlmcTJKZXhXZnZULUxfV3Z4SmgxRmR6ZVNIUXk0MTBvdFpnMzE4NnBjeVRZSGtmaml4Ukc5dkFIZHh3SDktRU90WDdfbnpHemlWRER3NDZNNzNMQy1L?oc=5)
<!--README_FEED:END-->
