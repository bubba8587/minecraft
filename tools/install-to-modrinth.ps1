# dte:A3
# Build the playable mods and install them into a local Modrinth App profile for hands-on testing.
#
#   pwsh tools/install-to-modrinth.ps1                      # profile "mod testing"
#   pwsh tools/install-to-modrinth.ps1 -Profile "other"     # another profile (must be Fabric on the repo's Minecraft version)
#   pwsh tools/install-to-modrinth.ps1 -ResetWorld          # also write over the test worlds (flat and natural) with fresh copies (no old copy kept)
#
# Also copies the test world (testworlds/bubba-test-world) into the profile's saves as "Bubba Test World" when missing,
# and the owner's own game settings (tools/sync-minecraft-settings.ps1).
#
# Uses a JDK from JAVA_HOME if set, otherwise the Java 25 runtime the Modrinth App downloaded (Gradle fetches a full
# JDK 25 for compiling by itself). Replaces older copies of our jars in the profile; leaves every other mod alone.
param(
	[string]$Profile = "mod testing",
	[string[]]$Mods = @("bubbas_reworked_mobs", "bubbas_extra_recipes", "bubbas_infused_gems", "bubbas_reworked_beacons", "bubbas_totems_artifacts", "bubbas_reworked_mining", "bubbas_world_tweaks"),
	[switch]$ResetWorld
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
	$tasks = @($Mods | ForEach-Object { ':' + $_ + ':assemble' })
	& .\gradlew.bat @tasks --console=plain -q
	if ($LASTEXITCODE -ne 0) { throw "Build failed." }
} finally { Pop-Location }

New-Item -ItemType Directory -Force $target | Out-Null
foreach ($m in $Mods) {
	Get-ChildItem $target -Filter "$m-*.jar" | Remove-Item -Confirm:$false
	Get-ChildItem (Join-Path $root "mods\$m\build\libs") -Filter "$m-*.jar" | Where-Object Name -notmatch "sources" |
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

# Our 32x textures (B16): the combined resource pack resourcepacks/bubbas_32x, built by tools/pack.py, copied into the
# profile's resourcepacks/ (replacing only older copies of it) and switched on in options.txt, above any Classic
# Faithful 32x Jappa (file/CF32J...) entry. Old built-in entries (bubbas_<mod>:bubbas_<mod>_32x) are dropped.
$packScript = Join-Path $root "tools\pack.py"
if (Get-Command python -ErrorAction SilentlyContinue) { & python $packScript build bubbas_32x } else { & py -3 $packScript build bubbas_32x }
if ($LASTEXITCODE -ne 0) { throw "Building the 32x resource pack failed." }
$packVersion = (Get-Content (Join-Path $root "resourcepacks\bubbas_32x\VERSION") -TotalCount 1).Trim()
$packZip = Join-Path $root "build\packs\resourcepacks\bubbas_32x-$packVersion.zip"
$packDir = Join-Path $modrinth "profiles\$Profile\resourcepacks"
New-Item -ItemType Directory -Force $packDir | Out-Null
Get-ChildItem $packDir -Filter "bubbas_32x-*.zip" | Remove-Item -Confirm:$false
Copy-Item $packZip -Destination $packDir
Write-Output "  resourcepacks\bubbas_32x-$packVersion.zip"

$options = Join-Path $modrinth "profiles\$Profile\options.txt"
$gameRunning = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
	Where-Object { $_.CommandLine -and $_.CommandLine.Contains((Join-Path $modrinth "profiles\$Profile")) }
if ($gameRunning) {
	Write-Output "Minecraft is running on '$Profile': the 32x pack was copied but not switched on (the game would undo it on exit)."
} elseif (Test-Path $options) {
	$lines = @(Get-Content $options)
	$i = [array]::FindIndex($lines, [Predicate[string]] { param($l) $l.StartsWith("resourcePacks:") })
	$packs = if ($i -ge 0) { @(ConvertFrom-Json $lines[$i].Substring("resourcePacks:".Length)) } else { @("vanilla", "fabric") }
	$entry = "file/bubbas_32x-$packVersion.zip"
	$kept = @($packs | Where-Object { $_ -notmatch '^bubbas_[a-z0-9_]+:bubbas_[a-z0-9_]+_32x$' -and $_ -notmatch '^file/bubbas_32x-.*\.zip$' })
	$cf = -1
	for ($k = 0; $k -lt $kept.Count; $k++) { if ($kept[$k] -like "file/CF32J*") { $cf = $k } }
	$new = if ($cf -ge 0) { @($kept[0..$cf]) + $entry + @($kept | Select-Object -Skip ($cf + 1)) } else { $kept + $entry }
	$line = "resourcePacks:" + (ConvertTo-Json @($new) -Compress)
	if ($i -ge 0) { $lines[$i] = $line } else { $lines += $line }
	Set-Content -Path $options -Value $lines -Encoding utf8NoBOM
	Write-Output "  options.txt: $entry switched on"
} else {
	Write-Output "No options.txt in '$Profile' yet: start the game once, then run this again to switch the 32x pack on."
}

# The test world (specs/testworld.md): copy the pristine world into saves/ if the profile has none. -ResetWorld
# writes over the owner's copy with a fresh one (owner: no old copies kept).
$pristine = Join-Path $root "testworlds\bubba-test-world"
$saves = Join-Path $modrinth "profiles\$Profile\saves"
$copy = Join-Path $saves "Bubba Test World"
if (Test-Path (Join-Path $pristine "level.dat")) {
	New-Item -ItemType Directory -Force $saves | Out-Null
	if ($ResetWorld) {
		# Owner: no old copies; a reset writes over the test world. Also clears backups left by earlier versions of this script.
		Get-ChildItem $saves -Directory -Filter "Bubba Test World (old *)" | Remove-Item -Recurse -Force
		if (Test-Path $copy) {
			Remove-Item $copy -Recurse -Force
			Write-Output "Wrote over your test world with a fresh copy"
		}
	}
	if (-not (Test-Path $copy)) {
		Copy-Item $pristine $copy -Recurse
		Write-Output "Test world copied to saves\Bubba Test World"
	} else {
		$new = Join-Path $pristine "BUILD_INFO.txt"; $have = Join-Path $copy "BUILD_INFO.txt"
		if ((Test-Path $new) -and (-not (Test-Path $have) -or (Get-Item $new).LastWriteTime -gt (Get-Item $have).LastWriteTime)) {
			Write-Output "Note: a newer test world has been built. Your copy was kept; run with -ResetWorld to write over it with the new one."
		}
	}
} else {
	Write-Output "No test world built yet (python tools/build-testworld.py); skipped."
}

# The natural test world (specs/testworld.md "Natural world"): the same, as its own save "Bubba Natural World".
$naturalPristine = Join-Path $root "testworlds\bubba-natural-world"
$naturalCopy = Join-Path $saves "Bubba Natural World"
if (Test-Path (Join-Path $naturalPristine "level.dat")) {
	New-Item -ItemType Directory -Force $saves | Out-Null
	if ($ResetWorld -and (Test-Path $naturalCopy)) {
		Remove-Item $naturalCopy -Recurse -Force
		Write-Output "Wrote over your natural test world with a fresh copy"
	}
	if (-not (Test-Path $naturalCopy)) {
		Copy-Item $naturalPristine $naturalCopy -Recurse
		Write-Output "Natural test world copied to saves\Bubba Natural World"
	} else {
		$new = Join-Path $naturalPristine "SPOTS.txt"; $have = Join-Path $naturalCopy "SPOTS.txt"
		if ((Test-Path $new) -and (-not (Test-Path $have) -or (Get-Item $new).LastWriteTime -gt (Get-Item $have).LastWriteTime)) {
			Write-Output "Note: a newer natural test world has been built. Your copy was kept; run with -ResetWorld to write over it."
		}
	}
}

# The owner's own game settings (keybinds, video, sound) into the profile.
& (Join-Path $PSScriptRoot "sync-minecraft-settings.ps1") -Profile $Profile
