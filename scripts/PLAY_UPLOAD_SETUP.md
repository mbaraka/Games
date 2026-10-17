# One-time setup: uploading to Play Console via script

This has to be done once, by you, in your own Google Cloud and Play Console accounts - it
grants a "robot" account permission to publish on your behalf, so only you should do it.

## 1. Create a Google Cloud project and service account

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and create a new project
   (or reuse one) - e.g. name it "game-hub-publishing".
2. In that project, go to **APIs & Services → Library**, search for **"Google Play Android
   Developer API"**, and click **Enable**.
3. Go to **IAM & Admin → Service Accounts → Create Service Account**.
   - Name: e.g. `play-console-uploader`.
   - No project-level role needs to be granted here - permissions are granted inside Play
     Console instead (next section).
4. Open the new service account → **Keys** tab → **Add Key → Create new key → JSON**.
   This downloads a `.json` file - **treat it like a password**. Save it as:
   ```
   D:\Project\Game\scripts\play-service-account.json
   ```
   This path is already gitignored - it will never be committed.

## 2. Link it in Play Console

1. In [Play Console](https://play.google.com/console), go to **Setup → API access**.
2. Under "Service accounts", find the one you just created (it appears automatically once
   the Cloud project is linked - link the Cloud project here if prompted) and click
   **Manage Play Console permissions**.
3. Grant it:
   - **App permissions**: select Game Hub specifically (not all apps, unless you want that).
   - **Account permissions / Release permissions**: at minimum, "Release to testing tracks".
     Only add **"Release to production"** once you're ready for this script to be able to
     publish publicly too - you can come back and add that permission later, it's not
     all-or-nothing.
4. Save.

It can take a few minutes for the new permissions to propagate.

## 3. Create your local config

```bash
cd D:/Project/Game/scripts
cp play_upload_config.example.json play_upload_config.json
```

Edit `play_upload_config.json` if your service account JSON file ends up at a different path
than the default. This file is also gitignored.

## 4. Install the Python dependencies (one time)

```bash
python3 -m pip install -r D:/Project/Game/scripts/requirements.txt
```

## 5. Try it - dry run first

```bash
python3 D:/Project/Game/scripts/upload_to_play.py \
  --aab "D:/Project/Game/app/release/baraka-0.1.0-1-release.aab" \
  --track internal
```

Without `--commit`, this validates everything and tells you what it *would* do, then discards
the draft - nothing gets published. Once you're happy, re-run the same command with `--commit`
added to actually publish it.

When `--commit` actually publishes, the script also creates an annotated git tag on the current
commit (e.g. `play-v0.1.0-1`) so every real release has a permanent marker tying it back to the
exact source - it's local only, so push it yourself afterwards with the `git push origin <tag>`
command it prints.

## Promoting to production later

When you're ready to go public, make sure the service account has the "Release to production"
permission (step 2 above), then run:

```bash
python3 scripts/upload_to_play.py --aab app/release/baraka-X.Y.Z-N-release.aab --track production --commit
```

Or, to do a staged rollout (e.g. 20% of users first):

```bash
python3 scripts/upload_to_play.py --aab app/release/baraka-X.Y.Z-N-release.aab --track production --rollout 0.2 --commit
```
