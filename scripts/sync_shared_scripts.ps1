# Copies this project's canonical upload_to_play.py out to sibling app projects that use the
# same script. Run this after editing scripts/upload_to_play.py here.
#
# Not a symlink/hardlink on purpose: those silently break the moment either project's copy is
# saved by a normal editor (atomic write-then-rename replaces the link), so a plain copy - run
# deliberately after a real change - is the more honest sync mechanism.

$source = "$PSScriptRoot\upload_to_play.py"
$targets = @(
    "D:\Project\PrayerCompanion\scripts\upload_to_play.py"
)

foreach ($target in $targets) {
    if (Test-Path (Split-Path $target)) {
        Copy-Item $source $target -Force
        Write-Output "Synced to $target"
    } else {
        Write-Warning "Skipped (directory not found): $target"
    }
}
