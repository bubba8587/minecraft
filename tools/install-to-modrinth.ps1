# dte:A3
# Build the playable mods and install them into a local Modrinth App profile for hands-on testing.
#
#   pwsh tools/install-to-modrinth.ps1                      # profile "mod testing"
#   pwsh tools/install-to-modrinth.ps1 -Profile "other"     # another profile (must be Fabric on the repo's Minecraft version)
#
# Uses a JDK from JAVA_HOME if set, otherwise the Java 25 runtime the Modrinth App downloaded (Gradle fetches a full
# JDK 25 for compiling by itself). Replaces older copies of our jars in the profile; leaves every other mod alone.
param(
	[string]$Profile = "mod testing",
	[string[]]$Mods = @("bubbas_reworked_mobs", "bubbas_crafting_tweaks")
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$modrinth = Join-Path $env:APPDATA "ModrinthApp"
$target = Join-Path $modrinth "profiles\$Profile\mods"
if (-not (Test-Path (Join-Path $modrinth "profiles\$Profile"))) { throw "No Modrinth profile named '$Profile'." }

if (-not $env:JAVA_HOME) {
	$jre = Get-ChildItem (Join-Path $modrinth "meta\java_versions") -Directory -ErrorAction SilentlyContinue |
		Where-Object Name -match '25' | Select-Object -First 1
	if (-not $jre) { throw "No Java 25 found: set JAVA_HOME, or launch a 26.x profile once so the Modrinth App downloads one." }
	$env:JAVA_HOME = $jre.FullName
}

Push-Location $root
try {
	$tasks = $Mods | ForEach-Object { ":${_}:assemble" }
	& .\gradlew.bat @tasks --console=plain -q
	if ($LASTEXITCODE -ne 0) { throw "Build failed." }
} finally { Pop-Location }

New-Item -ItemType Directory -Force $target | Out-Null
foreach ($m in $Mods) {
	Get-ChildItem $target -Filter "$m-*.jar" | Remove-Item -Confirm:$false
	Get-ChildItem (Join-Path $root "mods\$m\build\libs") -Filter "*.jar" | Where-Object Name -notmatch "sources" |
		Copy-Item -Destination $target
}

# Fabric API matching gradle.properties, from Modrinth (SHA-512 checked), if the profile doesn't have one.
if (-not (Get-ChildItem $target -Filter "fabric-api-*.jar")) {
	$fapi = ((Get-Content (Join-Path $root "gradle.properties")) -match '^fabric_api_version=')[0].Split('=')[1].Trim()
	$mc = ((Get-Content (Join-Path $root "gradle.properties")) -match '^minecraft_version=')[0].Split('=')[1].Trim()
	$ua = @{ 'User-Agent' = 'minecraft-repo-local-setup' }
	$q = [uri]::EscapeDataString("[`"$mc`"]"); $l = [uri]::EscapeDataString('["fabric"]')
	$v = (Invoke-RestMethod "https://api.modrinth.com/v2/project/P7dR8mSH/version?game_versions=$q&loaders=$l" -Headers $ua) |
		Where-Object version_number -eq $fapi | Select-Object -First 1
	$f = $v.files | Where-Object primary | Select-Object -First 1
	$dest = Join-Path $target $f.filename
	Invoke-WebRequest $f.url -OutFile $dest -Headers $ua
	if ((Get-FileHash $dest -Algorithm SHA512).Hash.ToLower() -ne $f.hashes.sha512) { Remove-Item $dest; throw "Fabric API checksum mismatch." }
}

Write-Output "Installed into '$Profile':"
Get-ChildItem $target -Filter "*.jar" | ForEach-Object { "  $($_.Name)" }
