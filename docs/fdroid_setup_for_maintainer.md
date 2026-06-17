# F-Droid setup - instructions for the upstream maintainer

Audience: a maintainer of `Aster-Privacy/Aster-Android` who holds the release
signing key and can publish releases and edit the fdroiddata merge request.

## Why the first submission stalled

F-Droid builds every app entirely from source on its own servers. The earlier
attempt could not pass because the native crypto library
(`libaster_crypto_ffi.so`) was a **prebuilt binary**: the Rust source was not in
this repo and `core-crypto/src/main/jniLibs/` (where the `.so` lived) is
gitignored. A clean checkout therefore produced an APK with **no** crypto
library, which both (a) is non-functional and (b) does not match the
hand-signed reference binary attached to the release (the reference had the
`.so`, an F-Droid build did not). That mismatch is the "not reproducible"
failure.

(The previous version of this doc claimed the build was already reproducible.
That comparison was made between two builds that both had the prebuilt `.so`
dropped in by hand; it was not a true from-source build. It was wrong.)

## What changed

The crypto core is now built from source as part of the build:

- `rust/aster-crypto-ffi/` - the Rust JNI crate, vendored into this repo
  (AGPL-3.0; self-contained, no secrets). The underlying algorithms were
  already public in `Aster-Mail/aster-crypto`.
- `rust/aster-crypto-ffi/scripts/build_android.sh` - compiles the three ABIs
  via `cargo-ndk` into `core-crypto/src/main/jniLibs/`.
- `rust-toolchain.toml` + `Cargo.lock` pin the toolchain and dependencies.
- `.github/workflows/build.yml` runs the cargo-ndk step before
  `./gradlew assemble`, so CI APKs are complete and built from source.

The `.so` files remain gitignored - they are build output, never committed.

## The two flavors (unchanged)

`app/build.gradle.kts` defines `full` (GMS/Firebase; the `Aster-Mail.apk`
published on GitHub) and `fdroid` (FOSS, no GMS). F-Droid must build `fdroid`.

## fdroiddata recipe

The build needs Rust + cargo-ndk steps. `subdir` is `app`, so build-step paths
are relative to `app/` (the crate is at `../rust/aster-crypto-ffi`). The
`build_android.sh` script resolves the output dir from its own location, so it
writes to the right `jniLibs` regardless of the working directory.

```yaml
Builds:
  - versionName: 0.6.73
    versionCode: 80
    commit: ca43a6e8e0647b580cf52e8058393f4e36e8f34e
    subdir: app
    ndk: r27c
    prebuild:
      - rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
      - cargo install cargo-ndk --locked --version 4.1.2
    build:
      - ANDROID_NDK_HOME="$$NDK$$" bash ../rust/aster-crypto-ffi/scripts/build_android.sh
    gradle:
      - fdroid
```

Two known recipe details to settle with F-Droid's buildserver on the first run:
- **NDK version**: pin `ndk:` to the version whose output you want to reproduce.
  `r27c` matches NDK 27.1.x used locally; adjust if F-Droid prefers another.
- **Offline build**: F-Droid restricts network during the build. If `cargo`
  cannot fetch crates, run `cargo vendor` in `rust/aster-crypto-ffi` and commit
  the vendor dir + a `.cargo/config.toml`, or use F-Droid's rust srclib support.

### Track B (recommended to ship first): F-Droid builds and signs

Simplest reliable path. No reference binary, no signing key anywhere, no
reproducibility comparison - F-Droid just needs the from-source build above to
succeed. Use the `Builds:` block as-is, with **no** `Binaries` and **no**
`AllowedAPKSigningKeys`:

```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags
```

Trade-off: F-Droid signs with its own key, so users cannot cross-update between
the F-Droid build and your GitHub/Play builds, and there is no "reproducible"
badge.

### Track A (optional enhancement): reproducible + developer-signed

Keeps your signature across channels and earns the reproducible badge, but
requires the from-source build to be **byte-reproducible** on F-Droid's
servers - confirm this with one build round-trip before relying on it.

Add to the recipe:

```yaml
Binaries: https://github.com/Aster-Privacy/Aster-Android/releases/download/v%v/Aster-Mail-fdroid-%v.apk
AllowedAPKSigningKeys: 88b0a8a6fb94ee73a454a0f92732bd408e92e51ec1e9556744e5f00556100977
```

Per release, publish a reference binary that is **built in CI** (clean room,
matches F-Droid's environment) and **signed locally** (the key never goes into
public CI):

1. Run the `release_fdroid` workflow on the tag; download the `fdroid-unsigned`
   artifact (`app-fdroid-release-unsigned.apk`).
2. On the release machine, sign it locally:
   ```bash
   zipalign -p -f 4 app-fdroid-release-unsigned.apk aligned.apk
   apksigner sign --ks keystore/aster-mail-upload-v3.jks \
     --ks-key-alias aster-mail --out Aster-Mail-fdroid-<version>.apk aligned.apk
   apksigner verify --print-certs Aster-Mail-fdroid-<version>.apk
   ```
   The cert SHA-256 must be `88b0a8a6...0977` (matches `AllowedAPKSigningKeys`).
3. Attach `Aster-Mail-fdroid-<version>.apk` to the GitHub Release for that tag.

Do **not** add `KEYSTORE_*` secrets to this public repo: that key also signs
your Play/GitHub builds, and anyone with repo write/Actions access could
exfiltrate it. Local signing keeps it off CI.

## Per-release discipline

Every release builds the native crypto from source automatically (CI). For
Track A, also publish the locally-signed `Aster-Mail-fdroid-%v.apk` at the
`v%v` URL, built from the exact tagged commit.
