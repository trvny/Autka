# Autka — open work

Tracks the remaining product gaps and the next useful work that does not depend on
scraping or an unlicensed marketplace feed. See `DATA_SOURCES.md` for the sourcing
boundary, partner paths and source acceptance checklist.

## Product blockers

1. **Compliant live catalogue.** Real marketplace adapters remain disabled until a
   licensed, partner, seller-provided or otherwise authorized feed is available. Mock data
   stays debug/test-only. Until then the production catalogue can be empty and the app
   relies on external marketplace deep-links.

2. **Normalized price + scalable pagination.** Native prices are mixed PLN/EUR/USD.
   Android currently requests an atomic `complete=true` set and applies NBP conversion,
   price filters and price sorting locally. Before the first large live catalogue, add a
   normalized-price field with rate provenance/timestamp plus cursor pagination and tests
   proving cross-currency ordering across page boundaries.

3. **Authoritative import inputs.** The landed-cost calculator is intentionally an
   estimate. Customs classification/origin relief, VAT edge cases and realistic shipping
   ranges need authoritative inputs before totals can be presented as more than indicative.

> A US-import broker partnership could close several gaps at once: inventory, shipping
> figures, lead attribution and a revenue path.

## Useful without a live provider

- **Saved searches.** Persist filters now; add price/new-listing alerts only once a live
  catalogue provides meaningful changes to watch.
- **VIN helper.** Explore a provider-independent VIN decode flow for US import candidates,
  preferring an official/authorized decoder and keeping decoded data separate from listings.
- **Import assumption presets.** If repeated calculator use makes it worthwhile, persist
  named local presets for shipping/customs/VAT assumptions while keeping the documented
  defaults and one-tap reset available.

## Deep-link verification

`feature/external/MarketplaceSearchLinks.kt` marks remaining uncertain values with
`TODO(verify)`. Do not guess parameters that silently degrade to an unfiltered page.

- Otomoto affiliate parameter after programme access.
- AutoUncle LPG / plug-in hybrid fuel values.
- AutoTrader.pl LPG / plug-in hybrid fuel values.
- Autoplac plug-in hybrid fuel value.
- Remaining parity-only transmission/fuel values where the code still says `TODO(verify)`.

Verified URL contracts and the AutoTrader US km-to-mile conversion have regression tests;
extend those tests whenever another parameter is confirmed.

## Engineering hygiene

- Keep `backend/src/lib/types.ts` and Android models in sync.
- Every enabled ingest adapter must declare snapshot vs delta semantics.
- Keep source-run history retention compatible with `/sources` health.
- If `/sources` later exposes expected cadence or failure streaks, derive stale/degraded
  status from those server fields rather than guessing provider-specific thresholds on Android.
- If `/sources` payload semantics broaden, add an explicit health-availability field instead
  of inferring backend health-query failure from `offerCount == null`.
- Consider a short-lived source-health cache if diagnostics should remain useful offline
  or across navigation; keep manual refresh network-backed.
- Split import-calculator input parsing/validation out of `ImportCalculatorScreen` before
  adding more fields; editable assumptions pushed the composable past the detekt complexity
  threshold.
- Add Room schema export plus a real migration test in CI before the next non-trivial DB
  migration; do not manufacture historical schema JSON by hand.
- Add normalized-price pagination tests when that backend work lands.
