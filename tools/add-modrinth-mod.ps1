# dte:A3
# Add an outside mod from Modrinth to a local test profile (not to our mods' dependencies), with its required
# dependencies, for the owner's playtesting.
#
#   pwsh tools/add-modrinth-mod.ps1 <slug> [<slug> ...]            # into profile "mod testing"
#   pwsh tools/add-modrinth-mod.ps1 <slug> -Profile "other"
#
# Picks the newest Fabric build for the repo's Minecraft version (gradle.properties), downloads it with SHA-512
# checking, and does the same for each required dependency it doesn't already have. Outside mods it added are listed
# in the profile's .bubba-extra-mods.txt so they can be told apart from our own jars.
param(
	[Parameter(Mandatory = $true, Position = 0, ValueFromRemainingArguments = $true)][string[]]$Slugs,
	[string]$Profile = "mod testing"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mods = Join-Path $env:APPDATA "ModrinthApp\profiles\$Profile\mods"
if (-not (Test-Path (Split-Path $mods))) { throw "No Modrinth profile named '$Profile'." }
New-Item -ItemType Directory -Force $mods | Out-Null
$mc = ((Get-Content (Join-Path $root "gradle.properties")) -match '^minecraft_version=')[0].Split('=')[1].Trim()
$ua = @{ 'User-Agent' = 'minecraft-repo-local-setup' }
$api = "https://api.modrinth.com/v2"
$listFile = Join-Path (Split-Path $mods) ".bubba-extra-mods.txt"
$done = @{}

function Add-Mod([string]$idOrSlug, [int]$depth) {
	if ($done.ContainsKey($idOrSlug)) { return }
	$done[$idOrSlug] = $true
	$project = Invoke-RestMethod "$api/project/$idOrSlug" -Headers $ua
	if ($done.ContainsKey($project.id) -and $depth -gt 0) { return }
	$done[$project.id] = $true
	$q = [uri]::EscapeDataString("[`"$mc`"]"); $l = [uri]::EscapeDataString('["fabric"]')
	$version = Invoke-RestMethod "$api/project/$($project.id)/version?game_versions=$q&loaders=$l" -Headers $ua | Select-Object -First 1
	if (-not $version) { throw "$($project.title) has no Fabric build for Minecraft $mc." }
	$file = $version.files | Where-Object primary | Select-Object -First 1
	if (-not $file) { $file = $version.files[0] }
	$indent = "  " * $depth
	if (Test-Path (Join-Path $mods $file.filename)) {
		Write-Output "$indent$($project.title) $($version.version_number): already installed"
	} else {
		$dest = Join-Path $mods $file.filename
		Invoke-WebRequest $file.url -OutFile $dest -Headers $ua
		if ((Get-FileHash $dest -Algorithm SHA512).Hash.ToLower() -ne $file.hashes.sha512) { Remove-Item $dest; throw "Checksum mismatch for $($file.filename)." }
		Add-Content -Path $listFile -Value "$($project.slug)`t$($file.filename)" -Encoding utf8NoBOM
		Write-Output "$indent$($project.title) $($version.version_number): installed ($($file.filename))"
	}
	foreach ($dep in $version.dependencies | Where-Object { $_.dependency_type -eq 'required' -and $_.project_id }) {
		Add-Mod $dep.project_id ($depth + 1)
	}
}

foreach ($s in $Slugs) { Add-Mod $s 0 }
