#!/usr/bin/env bash
echo "Starting diameter-load-simulator"
echo ""
if [ ! -f build/libs/diameter-load-simulator.jar ]; then
  echo "Library not found, building..."
  echo ""
  ./gradlew clean assemble -q
fi
echo "Starting..."
echo ""
sleep 2
java -jar build/libs/diameter-load-simulator.jar
