# F-Droid setup - instructions for the upstream maintainer

Audience: a maintainer of `Aster-Privacy/Aster-Android` who holds the release
signing key and can publish releases and edit the fdroiddata merge request.

This documents what to set up so the app ships correctly on F-Droid. The
findings below were validated on a fork sandbox; the actions must be performed
on the upstream repo and its release process.

## TL;DR

- The published `Aster-Mail.apk` is the **`full` (GMS/Firebase) flavor**.
  F-Droid cannot and will not build that flavor.
- The app's **`fdroid` flavor** is the FOSS build for F-Droid. It was verified
  to be **reproducible** (see Evidence).
- Decide between two tracks:
  - **Track A (recommended): reproducible + developer-signed.** You publish a
    signed `fdroid`-flavor APK as the reference binary; F-Droid verifies it
    reproduces and ships your signed APK.
  - **Track B (simpler): F-Droid builds and signs.** No reference binary; F-Droid
    signs with its own key.

## Background: the two flavors

`app/build.gradle.kts` defines two product flavors under dimension
`distribution`:

- `full` - includes Google Play Services / Firebase Cloud Messaging. This is the
  APK published as `Aster-Mail.apk` on GitHub Releases.
- `fdroid` - the FOSS build, no GMS/Firebase. This is what F-Droid must use.

A byte comparison of the published `Aster-Mail.apk` (v0.6.69) against an
`fdroid`-flavor build of the same version showed the published APK contains
Firebase/Play-Services components, extra native libs, and extra drawables that
the `fdroid` flavor does not. They are intentionally different products. This is
why earlier reproducibility attempts against `Aster-Mail.apk` failed: it was the
wrong flavor, not a build problem.

## Evidence: the fdroid flavor is reproducible

Two independent `fdroid`-flavor builds of versionCode 76 (F-Droid's build server
and a clean GitHub Actions runner) were compared:

- 418 entries each; **417 byte-for-byte identical** - including `classes*.dex`,
  `resources.arsc`, every `res/*`, the native `.so` libraries, and the ART
  baseline profiles.
- The **only** differing file was `META-INF/version-control-info.textproto`, an
  AGP-injected record of the source commit. It differed solely because the two
  builds were of different commits; building the same tagged commit makes it
  match too.
- Timestamps are already normalized to the `1981-01-01` zip epoch by AGP.

Conclusion: the `fdroid` flavor reproduces. Track A is viable.

---

## Track A (recommended): reproducible + developer-signed

F-Droid builds the `fdroid` flavor from source, copies your signature from the
reference binary onto its build, confirms the rest is byte-identical, and ships
your signed APK. Users get an APK signed by your key, with the "reproducible"
badge.

### A1. Per release: publish a signed fdroid-flavor reference binary

The `fdroid` flavor is force-unsigned in gradle:

```kotlin
if (System.getenv("ASTER_UNSIGNED") != "1" && !is_fdroid_build) { ...signing... }
```

`is_fdroid_build` is always true for an fdroid task, so gradle will not sign it.
Sign it **after** building, with `apksigner`. On the release machine, for each
tagged release:

```bash
./gradlew assembleFdroidRelease
apksigner sign \
  --ks <release-keystore>.jks \
  --ks-key-alias aster-mail \
  --out Aster-Mail-fdroid.apk \
  app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk
apksigner verify --print-certs Aster-Mail-fdroid.apk
```

Notes:
- Use a **distinct filename** (`Aster-Mail-fdroid.apk`) so it coexists with the
  full-flavor `Aster-Mail.apk` on the same GitHub Release.
- Signing only adds the signing block; it does not disturb the reproducible
  content. F-Droid strips/copies the signature before comparing.
- The key may be your existing release key or a dedicated one. Whatever signs
  this APK is what `AllowedAPKSigningKeys` must list.

Attach `Aster-Mail-fdroid.apk` to the GitHub Release for that version tag.

### A2. Get the signing certificate SHA-256

`AllowedAPKSigningKeys` is the SHA-256 of the signing certificate (lowercase
hex, no colons):

```bash
apksigner verify --print-certs Aster-Mail-fdroid.apk
# read the "certificate SHA-256 digest" line
```

(For reference, the certificate on the current published builds is
`88b0a8a6fb94ee73a454a0f92732bd408e92e51ec1e9556744e5f00556100977`. Use the
value for whichever key actually signs the fdroid reference binary.)

### A3. fdroiddata recipe (the merge request)

Add `Binaries` and `AllowedAPKSigningKeys` to
`metadata/org.astermail.android.yml`. Field order matters - `fdroid rewritemeta`
enforces it: `Binaries` after `Repo`, `AllowedAPKSigningKeys` after `Builds`.

```yaml
Binaries: https://github.com/Aster-Privacy/Aster-Android/releases/download/v%v/Aster-Mail-fdroid.apk

Builds:
  - versionName: 0.6.69
    versionCode: 76
    commit: <exact tag commit>
    subdir: app
    gradle:
      - fdroid

AllowedAPKSigningKeys: <cert sha256 from A2>

AutoUpdateMode: Version
UpdateCheckMode: Tags
```

Pitfalls confirmed during testing:
- `%v` (versionName) is **required** in the `Binaries` URL but **forbidden** in
  `AutoUpdateMode` - use bare `Version` there.
- The `Binaries` URL must serve a **per-version, immutable** artifact (the `v%v`
  tag does this). Do not point it at a rolling "latest" asset.
- Run `fdroid rewritemeta` before committing to satisfy field ordering and
  trailing-newline checks.

### A4. Ongoing release discipline

Every release must publish the signed `fdroid`-flavor APK at the `v%v` URL,
built from the exact tagged commit with the `fdroid` flavor. If a release skips
it, F-Droid's reproducibility check for that version fails.

---

## Track B (simpler): F-Droid builds and signs

No reference binary, no key handling. F-Droid builds the `fdroid` flavor and
signs it with F-Droid's key.

Recipe has **no** `Binaries` and **no** `AllowedAPKSigningKeys`:

```yaml
Builds:
  - versionName: 0.6.69
    versionCode: 76
    commit: <exact tag commit>
    subdir: app
    gradle:
      - fdroid

AutoUpdateMode: Version
UpdateCheckMode: Tags
```

Trade-offs vs Track A:
- Simpler: nothing to publish per release, no key in the release flow.
- F-Droid's signature differs from your GitHub/Play signature, so users cannot
  cross-update between the F-Droid build and your own distribution.
- No "reproducible" badge.

---

## Recommendation

For a privacy-focused project, **Track A** is worth the extra release step: it
proves the published binary matches the source and lets users keep your
signature across channels. Track B is the safe fallback if maintaining the
per-release signed reference binary is not practical.

## Quick checklist (Track A)

- [ ] Release machine builds `assembleFdroidRelease` and signs it with
      `apksigner` as `Aster-Mail-fdroid.apk`.
- [ ] That APK is attached to the GitHub Release under the `v<version>` tag.
- [ ] `AllowedAPKSigningKeys` set to the signing cert SHA-256.
- [ ] `Binaries` points at the `v%v` URL of the signed fdroid APK.
- [ ] `AutoUpdateMode: Version` (bare, no `%v`).
- [ ] `fdroid rewritemeta` run; schema/lint/build pipeline green.
