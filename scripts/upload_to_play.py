#!/usr/bin/env python3
"""
Uploads a signed .aab to a Google Play Console track (internal/alpha/beta/production)
using the Google Play Developer API, instead of the Play Console web UI.

One-time setup required before this script can run at all - see
scripts/PLAY_UPLOAD_SETUP.md.

Usage:
    python scripts/upload_to_play.py --aab path/to/app.aab --track internal
    python scripts/upload_to_play.py --aab path/to/app.aab --track internal --notes "Bug fixes"
    python scripts/upload_to_play.py --aab path/to/app.aab --track production --rollout 0.2

Safety:
    - Defaults to a DRY RUN: builds the edit and shows what would happen, but does not
      commit it, so nothing is actually published. Pass --commit to actually publish.
    - Never promotes between tracks implicitly - "--track production" only works if you
      explicitly pass it; there is no default/automatic promotion from internal to public.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
VALID_TRACKS = ["internal", "alpha", "beta", "production"]
REPO_ROOT = Path(__file__).resolve().parent.parent

# Matches the naming convention app/build.gradle.kts uses for its versioned bundle output
# in app/release/, e.g. "baraka-0.1.0-1-release.aab" -> versionName=0.1.0 (versionCode comes
# from the upload response instead, since that's the authoritative value Play actually accepted).
AAB_NAME_RE = re.compile(r"^.+-(?P<version_name>[\d][\w.]*)-\d+-[A-Za-z]+\.aab$")

# Default release notes shown to testers/users if none are given on the command line.
# Keyed by Play Console locale code - add more languages here as needed.
DEFAULT_NOTES = {
    "en-US": "Bug fixes and improvements.",
}


def load_config(config_path: Path) -> dict:
    if not config_path.exists():
        sys.exit(
            f"Config file not found: {config_path}\n"
            "Create it from scripts/play_upload_config.example.json first - "
            "see scripts/PLAY_UPLOAD_SETUP.md."
        )
    return json.loads(config_path.read_text(encoding="utf-8"))


def tag_release(aab_name: str, version_code: int, track: str) -> None:
    """Tags the current commit so every published release has a permanent git marker.

    Only called after an actual --commit publish. A failure here is a warning, not a fatal
    error - the release is already live on Play, so we must not raise after this point.
    """
    match = AAB_NAME_RE.match(aab_name)
    version_name = match.group("version_name") if match else "unknown"
    tag = f"play-v{version_name}-{version_code}"
    message = f"Released {version_name} (versionCode {version_code}) to Play Console track '{track}'"

    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=REPO_ROOT, capture_output=True, text=True
    )
    if status.stdout.strip():
        print(
            f"Warning: working tree has uncommitted changes - tag {tag} will point at HEAD, "
            "which may not be exactly what was built into this AAB."
        )

    result = subprocess.run(
        ["git", "tag", "-a", tag, "-m", message], cwd=REPO_ROOT, capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"Warning: could not create git tag {tag}: {result.stderr.strip()}")
        return

    print(f"Tagged current commit as {tag}.")
    print(f"Push it so it isn't only local: git push origin {tag}")


def get_service(service_account_json: Path):
    if not service_account_json.exists():
        sys.exit(
            f"Service account key not found: {service_account_json}\n"
            "See scripts/PLAY_UPLOAD_SETUP.md for how to create one."
        )
    credentials = service_account.Credentials.from_service_account_file(
        str(service_account_json), scopes=SCOPES
    )
    return build("androidpublisher", "v3", credentials=credentials)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--aab", required=True, type=Path, help="Path to the signed .aab to upload")
    parser.add_argument("--track", required=True, choices=VALID_TRACKS, help="Play Console track to release to")
    parser.add_argument("--notes", help="Release notes (en-US) - overrides the default for this one upload")
    parser.add_argument(
        "--rollout",
        type=float,
        default=1.0,
        help="Fraction of users to roll out to (0.0-1.0). Only meaningful for production/beta staged rollouts.",
    )
    parser.add_argument(
        "--commit",
        action="store_true",
        help="Actually publish the edit. Without this flag, the script only validates and prints "
        "what it WOULD do, then discards the draft edit - nothing is published.",
    )
    parser.add_argument(
        "--draft",
        action="store_true",
        help="Stage the release as a draft instead of completed/in-progress. Needed the first "
        "time a track (e.g. a brand-new closed testing track with no testers/countries set "
        "yet) hasn't been activated - Play rejects a 'completed' release on an inactive "
        "track. Finish the rollout manually in the Play Console afterward.",
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path(__file__).parent / "play_upload_config.json",
        help="Path to the config file with package_name and service_account_json path",
    )
    args = parser.parse_args()

    if not args.aab.exists():
        sys.exit(f"AAB not found: {args.aab}")

    config = load_config(args.config)
    package_name = config["package_name"]
    service_account_json = Path(config["service_account_json"])

    service = get_service(service_account_json)
    edits = service.edits()

    print(f"Creating edit for {package_name}...")
    edit = edits.insert(packageName=package_name, body={}).execute()
    edit_id = edit["id"]

    try:
        print(f"Uploading {args.aab.name} ({args.aab.stat().st_size / 1024 / 1024:.1f} MB)...")
        media = MediaFileUpload(str(args.aab), mimetype="application/octet-stream")
        bundle_response = edits.bundles().upload(
            packageName=package_name, editId=edit_id, media_body=media
        ).execute()
        version_code = bundle_response["versionCode"]
        print(f"Uploaded as versionCode {version_code}")

        notes_en = args.notes or DEFAULT_NOTES["en-US"]
        release_notes = [{"language": "en-US", "text": notes_en}]
        for lang, text in DEFAULT_NOTES.items():
            if lang != "en-US":
                release_notes.append({"language": lang, "text": text})

        status = "draft" if args.draft else ("completed" if args.rollout >= 1.0 else "inProgress")
        track_body = {
            "releases": [
                {
                    "versionCodes": [str(version_code)],
                    "status": status,
                    "releaseNotes": release_notes,
                }
            ]
        }
        if not args.draft and args.rollout < 1.0:
            track_body["releases"][0]["userFraction"] = args.rollout

        edits.tracks().update(
            packageName=package_name, editId=edit_id, track=args.track, body=track_body
        ).execute()
        print(f"Staged release to track '{args.track}' (rollout={args.rollout}).")

        if args.commit:
            result = edits.commit(packageName=package_name, editId=edit_id).execute()
            print(f"COMMITTED. Edit id: {result['id']}")
            print(f"versionCode {version_code} is now live on track '{args.track}'.")
            tag_release(args.aab.name, version_code, args.track)
        else:
            edits.delete(packageName=package_name, editId=edit_id).execute()
            print()
            print("DRY RUN ONLY - edit was validated then discarded, nothing was published.")
            print("Re-run with --commit to actually publish this release.")

    except Exception:
        print("Error occurred - discarding the draft edit so it doesn't linger as a stray draft.")
        try:
            edits.delete(packageName=package_name, editId=edit_id).execute()
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
