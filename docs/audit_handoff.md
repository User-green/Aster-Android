# Audit handoff - Aster-Android

Static review, branch `fix/send-retry-duplicates`. Read-only; nothing built or run. Opus audit already done - this is a short list of concerns to reconcile against it, not a full report.

## Concerns

### 1. Data loss on DB upgrade (schema 4 -> 5, no migration)
- `core-storage/.../search/AsterDatabase.kt` bumped `version` to 5 and `PendingSendEntity` gained `sending_started_at_ms`.
- `app/.../di/StorageModule.kt:104,112` uses `fallbackToDestructiveMigration()` with no explicit `Migration(4,5)`.
- Effect: every existing user's local Room DB is **wiped** on this update - pending outbox sends and the decrypted-mail cache are dropped. Not a security bug; a shipping regression.
- Fix: add a `Migration(4,5)` that `ALTER TABLE pending_send_queue ADD COLUMN sending_started_at_ms INTEGER NOT NULL DEFAULT 0`, register via `addMigrations(...)`. Consider dropping the destructive fallback so future misses fail loud instead of wiping.

### 2. Dead recovery-mnemonic code (delete)
- `core-crypto/.../CryptoNative.kt`: `generate_recovery_key`, `RecoveryKeyResult`, `bytes_to_mnemonic`, `recovery_wordlist`; plus `routes.recovery_key` / `recovery_key_for` in `MainActivity.kt` and `ui/auth/recovery_key_screen.kt`.
- No callers. `bytes_to_mnemonic` is `word[byte % 224]` - modulo-biased AND a lossy byte->word map (not reversible to the key), so it is not a real recovery mnemonic. Live recovery uses the `ASTER-xxxx` codes in `AuthRepository`.
- Risk: none today, but delete before someone wires it up believing it round-trips a key.

### 3. Unguarded logs in crypto/session paths (low)
- `app/.../mail/MailRepository.kt:1467` `Log.d("AsterRatchet", "envelope detected has_keys=...")`; `mail/ratchet/RatchetEncryptor.kt:131`; `core-storage/.../SessionKeyStore.kt:113`.
- Not guarded by `BuildConfig.DEBUG`, so they run in release. Values seen are booleans/metadata, no key material or plaintext - but they leak operational metadata to logcat. Wrap in `BuildConfig.DEBUG` or drop.

## Reviewed, no action
- **Email HTML WebView** (`ui/mail/mail_detail_screen.kt` + `email_html_sanitizer.kt`): sanitizer strips script/iframe/object, `on*`, `javascript:`/`vbscript:`/`data:text/html`, `srcdoc`/`formaction`/`ping`, dangerous CSS; WebView blocks all navigation, no JS bridge, file access off, mixed-content never allowed. JS enabled but backed by the sanitizer. Solid defense-in-depth.
- **Crypto** (`CryptoNative.kt`, `ratchet/RatchetCrypto.kt`): AES-GCM w/ random nonces, PBKDF2 310k, full EC point on-curve + scalar range validation, key zeroization. No issues found.
- **SecurePrefs** (`core-storage/.../secure_prefs.kt`): EncryptedSharedPreferences (AES256-SIV/GCM); on failure throws `SecurePrefsUnavailableException` - **no plaintext fallback**; wipe+recreate only on genuine corruption. Good.
- **Manifest / transport**: one exported component (launcher), `allowBackup=false`, backup/transfer exclusion rules, cert pinning + cleartext disabled.

## Not fully verified
- Turnstile `AsterBridge` JS interface vs. the cross-origin Cloudflare challenge iframe (`ui/auth/turnstile_widget.kt`). Low risk - local asset shell, trusted origin - but the asset-loader client's navigation restriction was not traced end to end.
