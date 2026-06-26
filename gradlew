#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
APP_NAME="Gradle"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
MAX_FD=maximum
warn() { echo "$*" >&2; }
die() { echo; echo "$*"; echo; exit 1; } >&2
cygwin=false; msys=false; darwin=false; nonstop=false
case "$(uname)" in
  CYGWIN*) cygwin=true;;
  Darwin*) darwin=true;;
  MSYS*|MINGW*) msys=true;;
  NONSTOP*) nonstop=true;;
esac
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
  [ ! -x "$JAVACMD" ] && die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
else
  JAVACMD=java
  command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
fi
if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
  case $MAX_FD in
    max*) MAX_FD=$(ulimit -H -n 2>/dev/null) || warn "Could not query maximum file descriptor limit";;
  esac
  case $MAX_FD in
    ''|soft) :;;
    *) ulimit -n "$MAX_FD" || warn "Could not set maximum file descriptor limit to $MAX_FD";;
  esac
fi
eval "set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \"-Djava.security.manager=allow\" \"-Dorg.gradle.appname=$APP_NAME\" -classpath \"$CLASSPATH\" org.gradle.wrapper.GradleWrapperMain \"$@\""
exec "$JAVACMD" "$@"
