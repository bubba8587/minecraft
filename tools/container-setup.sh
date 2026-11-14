#!/usr/bin/env bash
# dte:B5,B6
# Prepare a fresh Linux container (cloud session or CI runner) to build and test this repo.
# Idempotent. Installs JDK 25, software Vulkan + Xvfb for client game tests, and, when
# Maven Central rate-limits us, a Gradle init script that uses Google's Central mirror.
set -euo pipefail

if [ ! -x /opt/jdk25/bin/java ]; then
	mkdir -p /opt/jdk25
	curl -sL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" \
		| tar xz --strip-components=1 -C /opt/jdk25
fi

if command -v apt-get >/dev/null && [ "$(id -u)" = 0 ]; then
	if ! dpkg -s mesa-vulkan-drivers xvfb libvulkan1 >/dev/null 2>&1; then
		apt-get update -qq && apt-get install -y -qq xvfb mesa-vulkan-drivers libvulkan1 >/dev/null
	fi
fi

if [ "${CENTRAL_MIRROR:-auto}" != off ]; then
	code=$(curl -s -o /dev/null -w '%{http_code}' https://repo.maven.apache.org/maven2/com/google/guava/guava/33.0.0-jre/guava-33.0.0-jre.pom || true)
	if [ "$code" != 200 ] || [ "${CENTRAL_MIRROR:-auto}" = on ]; then
		mkdir -p ~/.gradle/init.d
		cat > ~/.gradle/init.d/central-mirror.gradle <<'GRADLE'
// Maven Central rate-limits this machine, so rewrite it to Google's Central mirror.
def MIRROR = 'https://maven-central.storage-download.googleapis.com/maven2/'
def rewrite = { RepositoryHandler repos ->
	repos.withType(MavenArtifactRepository).configureEach { r ->
		def u = r.url.toString()
		if (u.contains('repo.maven.apache.org') || u.contains('repo1.maven.org')) r.url = MIRROR
	}
}
beforeSettings { s -> rewrite(s.pluginManagement.repositories); rewrite(s.buildscript.repositories) }
settingsEvaluated { s -> rewrite(s.dependencyResolutionManagement.repositories) }
allprojects { p -> rewrite(p.buildscript.repositories); rewrite(p.repositories) }
GRADLE
		echo "Maven Central answered $code; installed the Google mirror init script"
	fi
fi
echo "ready: export JAVA_HOME=/opt/jdk25"
