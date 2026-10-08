# Releasing

> Agents: the short step-by-step version is in [`AGENTS.md`](AGENTS.md).

Version scheme: `MAJOR.MINOR.PATCH`, major fixed at `1`.
- **MINOR** — a new game is added.
- **PATCH** — small fixes/tweaks to existing games.

## One-time setup: release signing key

Do this once, on your own machine (the private key never leaves it except as a GitHub secret):

```bash
keytool -genkeypair -v -keystore release.keystore -alias baraka-game -keyalg RSA -keysize 2048 -validity 10000
```

Then add these as **repo secrets** (Settings → Secrets and variables → Actions):

```bash
gh secret set RELEASE_KEYSTORE_BASE64 --body "$(base64 -w0 release.keystore)"
gh secret set RELEASE_KEYSTORE_PASSWORD --body "<the password you set>"
gh secret set RELEASE_KEY_ALIAS --body "baraka-game"
gh secret set RELEASE_KEY_PASSWORD --body "<the key password you set>"
```

Keep `release.keystore` somewhere safe outside the repo — losing it means future
updates can no longer be signed to match previously installed APKs.

## Cutting a release

```bash
git tag v1.1.0 -m "Add Space Invaders"
git push origin v1.1.0
```

Pushing a `vMAJOR.MINOR.PATCH` tag triggers `.github/workflows/release.yml`, which:
1. Builds a signed release APK with that version baked in.
2. Publishes a GitHub Release for the tag with the APK attached and auto-generated
   release notes (from merged PRs/commits since the last tag) — edit the release
   afterward on GitHub if you want to write custom notes.

Anyone can then grab the APK from the repo's **Releases** page and sideload it.
Because every build is signed with the same keystore, installing a new release
over an old one works as a normal update (no uninstall needed).
