#!/bin/bash

set -e

TARGET_DIR="./target/com.themis"
JAVA_OPTS="-Xmx2G"


check_java_is_installed() {
    if ! command -v java >/dev/null 2>&1; then
        echo "Error: Java is not installed or not in PATH" >&2
        exit 1
    fi
}

find_jar() {
    if [ ! -d "$TARGET_DIR" ]; then
        echo "Error: Target directory '$TARGET_DIR' not found" >&2
        exit 1
    fi

    JAR_FILE=$(find "$TARGET_DIR" -maxdepth 1 -name "*.jar" -type f -exec ls -t {} + | head -n 1)

    if [ -z "${JAR_FILE:-}" ]; then
        echo "Error: No JAR file found in '$TARGET_DIR'" >&2
        exit 1
    fi

    echo "Found JAR file: $JAR_FILE"
}


check_java_is_installed
find_jar


QUOTED_ARGS=()
for arg in "$@"; do
    QUOTED_ARGS+=("$(printf %q "$arg")")
done

echo "Starting application..."
java $JAVA_OPTS -jar "${JAR_FILE}" "$@"


exit_code=$?

if [ $exit_code -ne 0 ]; then
    echo "Application exited with code $exit_code" >&2
fi

exit $exit_code