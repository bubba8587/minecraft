# dte:A3
# Copy the owner's own Minecraft settings (keybinds, video, sound...) into the test profile.
#
#   pwsh tools/sync-minecraft-settings.ps1                         # into profile "mod testing", from the newest source
#   pwsh tools/sync-minecraft-settings.ps1 -From "<folder with options.txt>"   # pick the source
#   pwsh tools/sync-minecraft-settings.ps1 -Force                  # copy even if nothing changed
#
# Source: the most recently saved options.txt among .minecraft and the other Modrinth profiles (or -From).
# Everything is copied except the resource pack lists, which stay the test profile's own (so its bundled packs,
# e.g. Bubba's Extra Recipes', stay on). The source's "version:" line is kept so the game upgrades older settings.
# A few owner overrides ($overrides below, e.g. GUI scale 3) are applied on top. Re-copied on every run (the owner's main settings win over anything the game wrote in the test profile); refuses
# to run while Minecraft is running on the profile, because the game saves its settings back on exit.
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

# Minecraft writes its settings back when it exits, so syncing while the profile is running gets undone.
$running = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
	Where-Object { $_.CommandLine -and $_.CommandLine.Contains($targetDir) }
if ($running) {
	Write-Output "Minecraft is running on '$Profile': close it, then run this again (it would overwrite the settings on exit)."
	return
}

# The owner's own settings are the source of truth: re-copied on every run (the game's own writes in the test
# profile, e.g. on exit, otherwise win). Skip only when the file is already exactly what a sync produces.

$keepFromTarget = '^(resourcePacks|incompatibleResourcePacks):'
$targetKeep = if (Test-Path $target) { @(Get-Content $target | Where-Object { $_ -match $keepFromTarget }) } else { @() }
$merged = @(Get-Content $source.FullName | Where-Object { $_ -notmatch $keepFromTarget }) + $targetKeep

# The owner's overrides for the test profile, applied on top of whatever the source says (owner: GUI scale 3).
$overrides = [ordered]@{ guiScale = '3' }
foreach ($key in $overrides.Keys) {
	$line = "${key}:$($overrides[$key])"
	$merged = @($merged | Where-Object { $_ -notmatch "^${key}:" }) + $line
}

if (-not $Force -and (Test-Path $target) -and ((Compare-Object (Get-Content $target) $merged -SyncWindow 0) -eq $null)) {
	Write-Output "Settings already match $($source.FullName)."
	return
}
if (Test-Path $target) { Copy-Item $target "$target.before-sync" -Force }
Set-Content -Path $target -Value $merged -Encoding utf8NoBOM
Set-Content -Path $marker -Value $stamp -Encoding utf8NoBOM
Write-Output "Copied settings from $($source.FullName) into '$Profile' (resource packs kept as they were; previous file saved as options.txt.before-sync)."
