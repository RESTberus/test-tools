#!/bin/bash

echo "cd to /tool"
cd /tool

echo "Configuring the tool"
# Configure the tool
bash ./config.sh

echo  "Starting the tool in loop"
# Start RESTest in a loop
while true; do
  echo "Loop iteration begin"
  bash ./run.sh
  echo "Loop iteration end"
done
