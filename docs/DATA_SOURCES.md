# Data sources and marketplace integrations

Autka is designed to aggregate vehicle offers only when the project has a lawful,
reliable right to ingest and display them. This document is the source of truth for that
boundary, partner integration paths and the fallback used when a marketplace cannot be
part of the catalogue.

## The rule

Autka does **not** scrape marketplaces or reverse-engineer private endpoints.

There are two deliberately separate paths:

1. **Catalogue data** comes from licensed, partner-provided, seller-authorised or
   otherwise permitted feeds. It is normalized by the Cloudflare backend and served to
   Android as `CarOffer` data.
2. **Marketplace deep-links** stay on the Android device. They translate the active
   `SearchFilter` into the original marketplace's search URL and open that site. No
   listing data is ingested by Autka.

The implementation boundary is simple: anything that produces a `CarOffer` belongs in
backend ingestion. Anything that only produces a URL belongs in `feature/external`.

## Current state

- The mock source exists for local development and debug/demo use only.
- Production builds do not enable mock offers.
- No third-party marketplace catalogue should be enabled until its data rights and
  operational contract are clear.
- Unsupported marketplaces remain available through deep-links where the URL contract is
  sufficiently verified.

The Android source-status screen reads the backend's public-safe `/sources` metadata. Raw
provider errors and credentials stay server-side.

## Partner integration paths

A useful source does not need a bespoke enterprise API on day one. Autka can accommodate
several authorised delivery models.

### 1. Dealer or importer snapshot

A partner can publish JSON or CSV over HTTPS or place a snapshot in an agreed storage
location. The important part is a stable contract, not the transport.

A snapshot envelope can be as simple as:

```json
{
  "sourceId": "partner_slug",
  "generatedAt": "2026-08-10T06:00:00Z",
  "completeSnapshot": true,
  "offers": []
}
```

The records inside `offers` must provide enough information for the backend adapter to
normalize each listing into the current `CarOffer` model. At minimum that means a stable
source/native id, title, make, model, price with currency, fuel type, transmission, region,
original listing URL and an image list (which may be empty). Year, mileage, power,
location, timestamps, coordinates and thumbnail are optional in the current model but
improve the result when available.

The operational contract must also define update timestamps, expiry/deletion semantics and
explicit permission for any listing text or images Autka displays or caches.

### 2. Pull API or licensed feed

For a provider API, the backend adapter owns authentication, pagination, retries and
normalization. Secrets never ship in the Android app. The adapter must declare whether a
successful run represents a **complete snapshot** or a **delta** because cleanup semantics
are different.

### 3. Partner push

A trusted partner may push signed data or upload a snapshot to an agreed endpoint/storage
location. The same data-rights and snapshot/delta rules apply; changing transport does not
change the licensing boundary.

### 4. Connected seller account

Some marketplace APIs are intended for a business to manage its own adverts. Those can be
useful when a dealer explicitly connects an account, but permission to access one seller's
inventory must not be treated as permission to ingest the entire marketplace.

### 5. Broker, auction or logistics partnership

For the USA → Poland side, an importer or broker can be valuable even without a broad
listing catalogue. Authorised inventory, realistic shipping bands, sourcing services,
lead attribution and better landed-cost assumptions can all improve Autka independently.

## Deep-link fallback

`app/src/main/java/com/autka/feature/external/MarketplaceSearchLinks.kt` is the maintained
source of truth for marketplace URL builders. It currently covers search hand-offs across
Polish, European and US marketplaces, including local listing sites, aggregators and US
auction/dealer sites.

Do not duplicate every URL parameter in this document. The code carries per-provider
verification notes and marks uncertain values with `TODO(verify)`. A guessed parameter can
silently open an unfiltered page, so uncertain filters should be omitted or verified
against a real marketplace URL before being relied on.

Deep-links are a convenience, not an ingestion source. A marketplace may remain available
as a deep-link even after a licensed feed is added.

## Source research shortlist

The following are **research/partnership directions**, not claims that Autka currently has
redistribution rights or active credentials:

1. Direct dealer and US-importer feeds.
2. Commercial vehicle-data providers with explicit search/listing licences.
3. Marketplace or aggregator partner programmes, especially sources that can cover more
   than one underlying marketplace.
4. Connected-seller APIs for businesses that opt in with their own inventory.
5. Auction/broker relationships for US inventory and import logistics.

Previous research has included providers and programmes such as Auto.dev, mobile.de,
MarketCheck, AutoUncle B2B, OTOMOTO/OLX/AutoScout24 partner APIs, Manheim, IAA and Copart.
Treat that list as leads to re-check with the provider before implementation, not as a
statement that access or redistribution terms are currently available.

## Adapter acceptance checklist

Before enabling any real source, record at least:

- the contractual or otherwise explicit right to ingest and display the data;
- whether listing text and images may be cached and for how long;
- required attribution, branding and link-back rules;
- stable source ids plus deletion/expiry semantics;
- full-snapshot vs delta semantics;
- pagination, rate limits, retries and expected update cadence;
- native currency, tax inclusion and distance/location units;
- update timestamps and an expected freshness target;
- lead, affiliate or reporting requirements;
- production credentials stored only as backend secrets.

If those answers are unclear, keep the source disabled and use a deep-link instead.

## Moving a source from link-only to live data

When a lawful feed becomes available:

1. document its rights and operational contract using the checklist above;
2. add a backend `IngestSource` adapter and tests;
3. declare snapshot/delta cleanup semantics explicitly;
4. normalize into the shared backend/Android `CarOffer` model;
5. keep credentials and provider-specific failures server-side;
6. verify source health and expiry behavior before enabling production ingestion;
7. decide separately whether the marketplace deep-link remains useful as an additional
   route to the original site.

See [`backend/README.md`](../backend/README.md) for ingestion mechanics and
[`ARCHITECTURE.md`](ARCHITECTURE.md) for the app/backend boundary.
