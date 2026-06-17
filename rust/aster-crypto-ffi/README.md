# aster-crypto-ffi (Android JNI crypto)

This crate is the native crypto core for the Android app. It is compiled from
source into `libaster_crypto_ffi.so` and packaged into the APK. The JNI symbols
here back `core-crypto/.../CryptoNative.kt`.

It is self-contained: it depends only on published crates (argon2, aes-gcm,
ed25519-dalek, hkdf, hmac, pbkdf2, sha2, base64, rand, zeroize, jni). It does
not read or embed any secret. The email-hash pepper is supplied at runtime by
the app (`set_hash_email_pepper`), never compiled in.

## Build

Requires the Android NDK and `cargo-ndk`:

```bash
cargo install cargo-ndk --locked --version 4.1.2
ANDROID_NDK_HOME=/path/to/ndk bash scripts/build_android.sh
```

This produces `libaster_crypto_ffi.so` for `arm64-v8a`, `armeabi-v7a`, and
`x86_64` into `core-crypto/src/main/jniLibs/`. Gradle then packages whatever is
in that directory; the `.so` files themselves are gitignored (build output,
never committed). CI (`.github/workflows/build.yml`) runs this step before
`./gradlew assemble`, so CI APKs are built entirely from source.

The toolchain is pinned in `rust-toolchain.toml` and dependencies in
`Cargo.lock` so the output is deterministic for F-Droid's reproducible build.
