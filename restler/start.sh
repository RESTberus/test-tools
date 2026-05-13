#!/bin/sh
set -e

# Restler binary is located at /RESTler/restler/Restler in the container.
cd /RESTler/restler || exit 1

# Compile the OpenAPI spec into grammar and dictionary.
./Restler compile --api_spec /app/spec.json

# Extract host and port from TARGET_URL (e.g., http://10.1.2.3:8080)
HOST=$(echo "$TARGET_URL" | awk -F/ '{print $3}' | cut -d: -f1)
PORT=$(echo "$TARGET_URL" | awk -F/ '{print $3}' | cut -d: -f2)

# Convert TIME_BUDGET_SECONDS to hours for Restler (expects hours)
TIME_BUDGET_HOURS=$(echo "$TIME_BUDGET_SECONDS / 3600" | bc -l)
# Ensure minimum of 0.01 hours (~36 seconds)
if [ -z "$TIME_BUDGET_HOURS" ] || [ "$(echo "$TIME_BUDGET_HOURS <= 0" | bc -l)" = "1" ]; then
    TIME_BUDGET_HOURS="0.01"
fi

while true; do
    ./Restler fuzz \
        --grammar_file ./Compile/grammar.py \
        --dictionary_file ./Compile/dict.json \
        --settings ./Compile/engine_settings.json \
        --no_ssl \
        --target_ip "$HOST" \
        --target_port "$PORT" \
        --time_budget "$TIME_BUDGET_HOURS" || true
done
