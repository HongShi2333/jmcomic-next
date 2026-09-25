# Security notes

This directory is a clean source distribution. It intentionally contains no
APK signing keystore, third-party service signing secret, proxy setting, or
user backup data.

The built-in API secrets are injected at build time through Gradle properties
or CI environment variables (`JM_BUILTIN_TOKEN_SECRET` and
`JM_APP_DATA_SECRET`). Keep signing keys outside the repository and rotate any
credentials that appeared in older public commits before publishing this copy.

Backup files are encrypted with AES-GCM using a random salt and IV. Password or
pattern verification remains the recovery fallback; biometric authentication
is only a convenience factor and is never the sole recovery credential.
