#!/usr/bin/env bash
# dte:A3
# Starts the live poster camera (specs/posters.md) under a virtual display; scripts go in with tools/posters/shoot.py.
set -euo pipefail
cd "$(dirname "$0")/../.."
[ -x /opt/jdk25/bin/java ] && export JAVA_HOME=/opt/jdk25
D=mods/postershots/build/run/posters
rm -rf "$D/queue" "$D/STATUS"; mkdir -p "$D/queue"
exec xvfb-run -a -s "-screen 0 2560x1440x24 +extension GLX +render -noreset" \
	./gradlew :postershots:runClientGameTest -PclientTests=posters "$@"
