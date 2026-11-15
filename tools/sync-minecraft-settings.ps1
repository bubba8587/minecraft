# dte:A3
# Copy the owner's own Minecraft settings (keybinds, video, sound...) into the test profile.
#
#   pwsh tools/sync-minecraft-settings.ps1                         # into profile "mod testing", from the newest source
#   pwsh tools/sync-minecraft-settings.ps1 -From "<folder with options.txt>"   # pick the source
#   pwsh tools/sync-minecraft-settings.ps1 -Force                  # copy even if nothing changed
#
# Source: the most recently saved options.txt among .minecraft and the other Modrinth profiles (or -From).
# Everything is copied except the resource pack lists, which stay the test profile's own (so its bundled packs,
# e.g. Bubba's Crafting Tweaks', stay on). The source's "version:" line is kept so the game upgrades older settings.
# Copies on first run, then again only when the source file changes (tracked in .bubba-settings-source), so
# settings changed inside the test profile are kept until the owner's main settings change.
param(
	[string]$Profile = "mod testing",
	[string]$From = "",
	[switch]$Force
)
$ErrorActionPreference = "Stop"
$appData = $env:APPDATA
$profiles = Join-Path $appData "ModrinthApp\profiles"
$targetDir = Join-Path $profiles $Profile
if (-not (Test-Path $targetDir)) { throw "No Modrinth profile named '$Profile'." }
$target = Join-Path $targetDir "options.txt"
$marker = Join-Path $targetDir ".bubba-settings-source"

if ($From) {
	$source = Get-Item (Join-Path $From "options.txt")
} else {
	$candidates = @(Join-Path $appData ".minecraft\options.txt") +
		(Get-ChildItem $profiles -Directory | Where-Object Name -ne $Profile | ForEach-Object { Join-Path $_.FullName "options.txt" })
	$source = $candidates | Where-Object { Test-Path $_ } | ForEach-Object { Get-Item $_ } |
		Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
if (-not $source) { Write-Output "No settings found to copy."; return }

$stamp = "$($source.FullName)|$($source.LastWriteTimeUtc.Ticks)"
if (-not $Force -and (Test-Path $target) -and (Test-Path $marker) -and ((Get-Content $marker -Raw).Trim() -eq $stamp)) {
	Write-Output "Settings already in sync with $($source.FullName)."
	return
}

$keepFromTarget = '^(resourcePacks|incompatibleResourcePacks):'
$targetKeep = if (Test-Path $target) { @(Get-Content $target | Where-Object { $_ -match $keepFromTarget }) } else { @() }
$merged = @(Get-Content $source.FullName | Where-Object { $_ -notmatch $keepFromTarget }) + $targetKeep

if (Test-Path $target) { Copy-Item $target "$target.before-sync" -Force }
Set-Content -Path $target -Value $merged -Encoding utf8NoBOM
Set-Content -Path $marker -Value $stamp -Encoding utf8NoBOM
Write-Output "Copied settings from $($source.FullName) into '$Profile' (resource packs kept as they were; previous file saved as options.txt.before-sync)."
