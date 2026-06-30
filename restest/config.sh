#!/bin/bash

echo "host=http://$HOST:$PORT" >> common/config.properties
cp /specifications/"$API".yaml common/swagger.yaml
cp /specifications/"$API"-openapi.json common/openapi.json

# Generate configuration based on the specification
java -jar restest-cli.jar -c common/openapi.json

# Necessary files for RESTest in src/main/resources
mkdir -p src/main/
mv common/ src/main/resources/