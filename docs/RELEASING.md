# Releasing Autka

This guide covers the repository's current GitHub release path and the extra work required
for store distribution. The release workflow is intentionally strict about signing: do not
cut a public release until the signing key is backed up and the required secrets are in
place.

## 1. Bump the app version

Update both values in `app/build.gradle.kts` to the intended next release. For example,
replace the placeholders below with the actual next values rather than copying them
literally:

```text
versionCode = <next integer>
versionName = "<next version>"
```

`versionCode` must increase for store releases. The `v*` Git tag must match the resulting
`versionName` exactly; the release workflow checks this and fails if they differ.

Add matching Fastlane changelog files for the new `versionCode`:

```text
fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
fastlane/metadata/android/pl-PL/changelogs/<versionCode>.txt
```

## 2. Signing key

Local `assembleRelease` builds may be unsigned when no signing environment is supplied.
The GitHub release workflow is different: it requires a valid release keystore and all four
Actions secrets before it will build/publish artifacts.

Generate the keystore once and keep it for the lifetime of the app identity:

```bash
keytool -genkeypair -v \
  -keystore autka-release.keystore \
  -alias autka \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass '<STORE_PASS>' -keypass '<KEY_PASS>' \
  -dname "CN=Autka, O=travny, C=PL"
```

Back up the keystore and passwords in more than one secure location before releasing.
Never commit the keystore; `.gitignore` excludes `*.keystore`.

Configure these Actions secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing alias, e.g. `autka` |
| `KEY_PASSWORD` | Key password |

`.github/workflows/release.yml` decodes the keystore, verifies the alias/passwords and only
then runs the signed APK/AAB build.

## 3. Create the GitHub release

After committing the version bump, tag the **exact** `versionName` from
`app/build.gradle.kts`:

```bash
git tag v<versionName>
git push origin v<versionName>
```

For example, if Gradle contains `versionName = "0.2.0"`, the tag is `v0.2.0`.

The workflow:

1. verifies the tag matches `versionName`;
2. validates the signing secrets;
3. builds release APK and AAB artifacts;
4. verifies that the APK/AAB are signed;
5. publishes a GitHub release with generated notes.

The workflow can also be dispatched manually for an **existing** `v*` tag.

## 4. Google Play

Use the AAB from the release workflow for Play distribution. Store text and graphics live
under `fastlane/metadata/android/` and can be used through Fastlane or copied into the Play
Console.

Before each store submission, re-check the Play data-safety declaration against the actual
release. The current Android manifest declares only:

- `INTERNET`;
- `ACCESS_NETWORK_STATE`.

The repository currently contains no advertising or analytics SDK integration, but that is
not a substitute for reviewing the deployed backend behavior and current store-policy
questions. Search/filter requests are sent to the Autka backend, so answer the form based on
what the released app and service actually do at that time.

## 5. F-Droid

Autka is structured to remain friendly to FOSS distribution: the app source is
Apache-2.0-licensed, the map uses osmdroid/OpenStreetMap rather than Google Maps, and local
release builds do not require proprietary signing tooling.

F-Droid eligibility and anti-feature labels are determined by F-Droid policy at submission
time, so verify them rather than treating this document as a guarantee. The hosted Autka
backend is a runtime network dependency even though its source is included in this repo.

Fastlane listing metadata already lives under `fastlane/metadata/android/`; an F-Droid
submission would still need its own `fdroiddata` build recipe and review.

## Release safety checklist

- `versionCode` increased and `versionName` matches the intended `v*` tag.
- EN/PL changelog entries added for the new `versionCode`.
- Release keystore and passwords are backed up.
- All four Actions signing secrets are present and still match the keystore.
- Manifest permission changes reviewed.
- `network_security_config.xml` still blocks production cleartext traffic; local emulator
  loopback exceptions remain development-only.
- Final release workflow is green and the published APK/AAB signatures were verified by CI.
- Store privacy/data-safety answers rechecked against the actual release.
