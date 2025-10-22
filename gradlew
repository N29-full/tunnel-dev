#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"
# Try to force JDK 21 for Gradle wrapper
if [ -z "$ORG_GRADLE_JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  JAVA21=$(/usr/libexec/java_home -v 21 2>/dev/null)
  if [ -n "$JAVA21" ]; then export ORG_GRADLE_JAVA_HOME="$JAVA21"; fi
fi
exec "${DIR}/gradlew-wrapper" "$@"
