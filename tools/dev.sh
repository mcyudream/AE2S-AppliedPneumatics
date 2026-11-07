#!/usr/bin/env bash
set -euo pipefail
applied_pneumatics_project=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
applied_pneumatics_local_jdk="$applied_pneumatics_project/../../.dev/jdk-25.0.5+8"
if [[ -n "${AP_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$AP_JAVA_HOME"
elif [[ -x "$applied_pneumatics_local_jdk/bin/java" ]]; then
    export JAVA_HOME="$applied_pneumatics_local_jdk"
fi
applied_pneumatics_proxy_args=()
applied_pneumatics_proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ "$applied_pneumatics_proxy" =~ ^https?://([^/:@]+):([0-9]+)/?$ ]]; then
    applied_pneumatics_proxy_args+=("-Dhttps.proxyHost=${BASH_REMATCH[1]}" "-Dhttps.proxyPort=${BASH_REMATCH[2]}")
    applied_pneumatics_proxy_args+=("-Dhttp.proxyHost=${BASH_REMATCH[1]}" "-Dhttp.proxyPort=${BASH_REMATCH[2]}")
fi
cd -- "$applied_pneumatics_project"
exec ./gradlew "${applied_pneumatics_proxy_args[@]}" "$@"
