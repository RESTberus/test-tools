#!/bin/bash

BASE_DIR="/tool"

# Run FT and RT in parallel, wait for both, then run RT-LLM
PARALLEL_GENERATORS=("FT" "RT")
POST_GENERATOR="RT-LLM"  # Set to empty string to disable
#POST_GENERATOR=""

echo "INFO: Launching in parallel: ${PARALLEL_GENERATORS[*]}"

pids=()
for GENERATOR in "${PARALLEL_GENERATORS[@]}"; do
    INSTANCE_DIR="$BASE_DIR/$GENERATOR"
    if [ -d "$INSTANCE_DIR" ]; then
        RUN_SCRIPT="$INSTANCE_DIR/run.sh"
        if [ -f "$RUN_SCRIPT" ] && [ -x "$RUN_SCRIPT" ]; then
            echo "INFO: Starting $GENERATOR..."
            bash "$RUN_SCRIPT" &
            pids+=("$!")
        else
            echo "ERROR: Instance $GENERATOR found, but '$RUN_SCRIPT' is missing or not executable. Skipping."
        fi
    else
        echo "ERROR: Instance $GENERATOR directory not found at '$INSTANCE_DIR'. Skipping."
    fi
done

# Wait for all parallel generators to finish
exit_codes=()
for pid in "${pids[@]}"; do
    if wait "$pid"; then
        exit_codes+=(0)
    else
        exit_codes+=($?)
    fi
done

echo "INFO: Parallel generators finished. Exit codes: ${exit_codes[*]}"

# Launch post generator after both have completed, if set
if [ -n "$POST_GENERATOR" ]; then
    GENERATOR="$POST_GENERATOR"
    INSTANCE_DIR="$BASE_DIR/$GENERATOR"
    RUN_SCRIPT="$INSTANCE_DIR/run.sh"
    if [ -d "$INSTANCE_DIR" ] && [ -f "$RUN_SCRIPT" ] && [ -x "$RUN_SCRIPT" ]; then
        echo "INFO: Starting $GENERATOR after FT and RT completion..."
        bash "$RUN_SCRIPT"
    else
        echo "ERROR: $GENERATOR not ready. Directory or script missing/not executable at '$RUN_SCRIPT'."
    fi
else
    echo "INFO: No post generator configured; skipping."
fi