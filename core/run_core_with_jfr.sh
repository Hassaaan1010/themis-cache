#!/bin/bash

# run_jfr.sh - Auto-incrementing JFR recording script
# Usage: ./run_jfr.sh [test_description] [options]
# Example: ./run_jfr.sh "bigger_eden_64m_regions" --heap 2g

# Configuration
JFR_DIR="jfr_recordings"
COUNTER_FILE="${JFR_DIR}/.run_counter"
JAR_PATH="target/core-0.1-SNAPSHOT-jar-with-dependencies.jar"

# Default JVM options
DEFAULT_HEAP_MIN="1g"
DEFAULT_HEAP_MAX="1g"
DEFAULT_REGION_SIZE="64m"
DEFAULT_IHOP="30"

# Parse arguments
TEST_DESC="${1:-default_test}"
HEAP_SIZE=""	# Caps heap size.
REGION_SIZE=""	# Sets the size of each G1 region. Size should be atleast double (or more) the maximum payload size expected to be delivered.  
IHOP="" 		# InitiatingHeapOccupancyPercent. default: 45. Triggers concurrent marking cycle when Old Gen reaches occupancy. helps prevent Old Gen from filling up and triggering expensive Full GC events.

shift  # Remove first argument (test description)

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
        --jar)
            JAR_PATH="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [test_description] [options]"
            echo ""
            echo "Options:"
            echo "  --heap SIZE       Set heap size (e.g., 1g, 2g)"
            echo "  --region SIZE     Set G1HeapRegionSize (e.g., 32m, 64m)"
            echo "  --ihop PERCENT    Set InitiatingHeapOccupancyPercent (e.g., 30)"
            echo "  --jar PATH        Path to JAR file"
            echo "  --help            Show this help message"
            echo ""
            echo "Example:"
            echo "  $0 \"bigger_eden\" --heap 2g --region 64m --ihop 30"
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

# Create JFR directory if it doesn't exist
mkdir -p "$JFR_DIR"

# Initialize or read counter
if [ ! -f "$COUNTER_FILE" ]; then
    echo "0" > "$COUNTER_FILE"
fi

CURRENT_RUN=$(cat "$COUNTER_FILE")
NEXT_RUN=$((CURRENT_RUN + 1))

# Generate filename
FILENAME="server_${NEXT_RUN}_${TEST_DESC}.jfr"
FULL_PATH="${JFR_DIR}/${FILENAME}"
CONFIG_FILE="${FULL_PATH%.jfr}.txt"


# Update counter
echo "$NEXT_RUN" > "$COUNTER_FILE"

# Display configuration
echo "======================================"
echo "JFR Recording Configuration"
echo "======================================"
echo "Run Number:    $NEXT_RUN"
echo "Test Name:     $TEST_DESC"
echo "Output File:   $FULL_PATH"
echo "Heap Size:     -Xms${HEAP_MIN} -Xmx${HEAP_MAX}"
echo "Region Size:   ${REGION}"
echo "IHOP:          ${IHOP_VAL}%"
echo "JAR Path:      $JAR_PATH"
echo "======================================"
echo ""


cat > "$CONFIG_FILE" << EOF
======================================
JFR Recording Configuration
======================================
Run Number:    $NEXT_RUN
Test Name:     $TEST_DESC
JFR File:      $FULL_PATH
Config File:   $CONFIG_FILE
Heap Size:     -Xms${HEAP_MIN} -Xmx${HEAP_MAX}
Region Size:   ${REGION}
IHOP:          ${IHOP_VAL}%
JAR Path:      $JAR_PATH
Timestamp:     $(date '+%Y-%m-%d %H:%M:%S')
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
    -Xms${HEAP_MIN} \
    -Xmx${HEAP_MAX} \
    -XX:+UseG1GC \
    -XX:G1HeapRegionSize=${REGION} \
    -XX:InitiatingHeapOccupancyPercent=${IHOP_VAL} \
    -XX:StartFlightRecording=settings=profile,filename="${FULL_PATH}" \
    -jar "$JAR_PATH"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "======================================"
    echo "Recording saved to: $FULL_PATH"
    echo "Next run will be: server_$((NEXT_RUN + 1))_*.jfr"
    echo "======================================"
else
    echo ""
    echo "Server exited with code: $EXIT_CODE"
fi

exit $EXIT_CODE

