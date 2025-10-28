#!/bin/bash

# run_jfr.sh - Auto-incrementing JFR recording script
# Usage: ./run_jfr.sh [test_description] [options]
# Example: ./run_jfr.sh "zgc_test" --heap 4g --gc zgc

# Configuration
JFR_DIR="jfr_recordings"
COUNTER_FILE="${JFR_DIR}/.run_counter"
JAR_PATH="target/core-0.1-SNAPSHOT-jar-with-dependencies.jar"

# Default JVM options
DEFAULT_HEAP_MIN="2g"
DEFAULT_HEAP_MAX="2g"
DEFAULT_REGION_SIZE="64m"
DEFAULT_IHOP="30"
DEFAULT_GC="g1"  # g1 or zgc

# Parse arguments
TEST_DESC="${1:-default_test}"
HEAP_SIZE=""
REGION_SIZE=""
IHOP=""
GC_TYPE=""
shift

# Parse optional flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --heap)
      HEAP_SIZE="$2"
      shift 2
      ;;
    --region)
      REGION_SIZE="$2"
      shift 2
      ;;
    --ihop)
      IHOP="$2"
      shift 2
      ;;
    --gc)
      GC_TYPE="$2"
      shift 2
      ;;
    --jar)
      JAR_PATH="$2"
      shift 2
      ;;
    --help)
      echo "Usage: $0 [test_description] [options]"
      echo ""
      echo "Options:"
      echo "  --heap SIZE      Set heap size (e.g., 2g, 4g, 8g)"
      echo "  --gc TYPE        Set GC type: g1 (default) or zgc"
      echo "  --region SIZE    Set G1HeapRegionSize (G1GC only, e.g., 32m, 64m)"
      echo "  --ihop PERCENT   Set InitiatingHeapOccupancyPercent (G1GC only, e.g., 30)"
      echo "  --jar PATH       Path to JAR file"
      echo "  --help           Show this help message"
      echo ""
      echo "Examples:"
      echo "  $0 \"g1gc_baseline\" --heap 2g --gc g1"
      echo "  $0 \"zgc_low_latency\" --heap 4g --gc zgc"
      echo "  $0 \"tuned_g1\" --heap 2g --gc g1 --region 64m --ihop 30"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Set defaults if not provided
HEAP_MIN="${HEAP_SIZE:-$DEFAULT_HEAP_MIN}"
HEAP_MAX="${HEAP_SIZE:-$DEFAULT_HEAP_MAX}"
REGION="${REGION_SIZE:-$DEFAULT_REGION_SIZE}"
IHOP_VAL="${IHOP:-$DEFAULT_IHOP}"
GC="${GC_TYPE:-$DEFAULT_GC}"

# Validate GC type
if [[ "$GC" != "g1" && "$GC" != "zgc" ]]; then
  echo "Error: Invalid GC type '$GC'. Must be 'g1' or 'zgc'"
  exit 1
fi

# Create JFR directory if it doesn't exist
mkdir -p "$JFR_DIR"

# Initialize or read counter
if [ ! -f "$COUNTER_FILE" ]; then
  echo "0" > "$COUNTER_FILE"
fi

CURRENT_RUN=$(cat "$COUNTER_FILE")
NEXT_RUN=$((CURRENT_RUN + 1))

# Generate filename
FILENAME="server_${NEXT_RUN}_${TEST_DESC}_${GC}.jfr"
FULL_PATH="${JFR_DIR}/${FILENAME}"
CONFIG_FILE="${FULL_PATH%.jfr}.txt"

# Update counter
echo "$NEXT_RUN" > "$COUNTER_FILE"

# Build GC-specific JVM options
if [[ "$GC" == "zgc" ]]; then
  GC_OPTS=(
    "-XX:+UseZGC"
    "-XX:+ZGenerational"
    "-XX:+AlwaysPreTouch"
    "-XX:-ZUncommit"
  )
  GC_INFO="ZGC (Generational)"
else
  GC_OPTS=(
    "-XX:+UseG1GC"
    "-XX:G1HeapRegionSize=${REGION}"
    "-XX:InitiatingHeapOccupancyPercent=${IHOP_VAL}"
  )
  GC_INFO="G1GC (Region: ${REGION}, IHOP: ${IHOP_VAL}%)"
fi

# Display configuration
echo "======================================"
echo "JFR Recording Configuration"
echo "======================================"
echo "Run Number: $NEXT_RUN"
echo "Test Name: $TEST_DESC"
echo "Output File: $FULL_PATH"
echo "Heap Size: -Xms${HEAP_MIN} -Xmx${HEAP_MAX}"
echo "GC Type: $GC_INFO"
echo "JAR Path: $JAR_PATH"
echo "======================================"
echo ""

# Save configuration
cat > "$CONFIG_FILE" << EOF
======================================
JFR Recording Configuration
======================================
Run Number: $NEXT_RUN
Test Name: $TEST_DESC
JFR File: $FULL_PATH
Config File: $CONFIG_FILE
Heap Size: -Xms${HEAP_MIN} -Xmx${HEAP_MAX}
GC Type: $GC_INFO
JAR Path: $JAR_PATH
Timestamp: $(date '+%Y-%m-%d %H:%M:%S')
======================================
EOF

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
  echo "Error: JAR file not found at $JAR_PATH"
  echo "Build the project first or specify correct path with --jar"
  exit 1
fi

# Run the application
echo "Starting server with JFR recording..."
echo "Press Ctrl+C to stop"
echo ""

java \
  -Xms${HEAP_MAX} \
  -Xmx${HEAP_MAX} \
  "${GC_OPTS[@]}" \
  -XX:StartFlightRecording=settings=profile,filename="${FULL_PATH}" \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+DebugNonSafepoints \
  -jar "$JAR_PATH"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
  echo ""
  echo "======================================"
  echo "Recording saved to: $FULL_PATH"
  echo "Config saved to: $CONFIG_FILE"
  echo "Next run will be: server_$((NEXT_RUN + 1))_*.jfr"
  echo "======================================"
else
  echo ""
  echo "Server exited with code: $EXIT_CODE"
fi

exit $EXIT_CODE
