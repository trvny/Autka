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

- **Indexed listing previews.** Build on the browser-only web fallback with optional
  lightweight results from a licensed search API behind the backend: title, source, URL and
  snippet only. Keep them separate from `CarOffer`, never crawl listing pages, and treat the
  original marketplace as authoritative.
- **VIN helper.** Explore a provider-independent VIN decode flow for US import candidates,
  preferring an official/authorized decoder and keeping decoded data separate from listings.
- **Import assumption presets.** If repeated calculator use makes it worthwhile, persist
  named local presets for shipping/customs/VAT assumptions while keeping the documented
  defaults and one-tap reset available.
- **Shareable import summary.** Let the calculator copy/share a compact landed-cost
  breakdown with the active assumptions and an explicit estimate disclaimer, without
  requiring an account or backend state.
- **Saved-search alerts.** Once a live catalogue provides meaningful changes to watch, add
  opt-in price-drop and new-listing alerts on top of the local saved searches.

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
- Keep `MarketplaceWebSearch` targets aligned with the external-marketplace provider intent;
  if the lists grow, centralize index-search metadata instead of maintaining parallel lists.
- Keep saved searches in lightweight DataStore storage while the list stays small; if usage
  grows materially, move to structured storage or an explicit user-visible limit rather than
  silently evicting saved searches.
- Add Room schema export plus a real migration test in CI before the next non-trivial DB
  migration; do not manufacture historical schema JSON by hand.
- Add normalized-price pagination tests when that backend work lands.
