#!/bin/bash

# restberus mounts the OAS at /app/spec.json and passes the target as TARGET_URL.
# FT/RT read the JSON copy; RT-LLM reads the "yaml" one (JSON is valid YAML, so the
# same file serves both).
echo "host=$TARGET_URL" >> common/config.properties
# restberus patch: disable per-attempt Allure report generation. The `allure generate`
# CLI is spawned and awaited on every FT/RT/RT-LLM iteration (AllureReportManager),
# which is pure overhead for restberus. Appended last so it overrides any upstream
# value in the vendor image (java.util.Properties: last occurrence wins).
echo "allure.report=false" >> common/config.properties
cp /app/spec.json common/swagger.yaml
cp /app/spec.json common/openapi.json

# Generate configuration based on the specification
java -jar restest-cli.jar -c common/openapi.json

# Necessary files for RESTest in src/main/resources
mkdir -p src/main/
mv common/ src/main/resources/