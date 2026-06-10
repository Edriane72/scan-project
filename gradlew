#!/usr/bin/env sh
set -e

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

DIRNAME="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar:$DIRNAME/gradle/wrapper/gradle-wrapper-shared.jar"
exec "$JAVACMD" -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
