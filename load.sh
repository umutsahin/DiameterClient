#!/usr/bin/env bash
PORT=3868
ADDRESS=127.0.0.1
LOAD=1
DURATION=1
opt=$(getopt -o "p:a:l:d:h" --long "port:,address:,load:,duration:,help" -- "$@")
for opt; do
  case "$opt" in
  -p | --port)
    PORT=$2
    shift 2
    ;;
  -a | --address)
    ADDRESS=$2
    shift 2
    ;;
  -l | --load)
    LOAD=$2
    shift 2
    ;;
  -d | --duration)
    DURATION=$2
    shift 2
    ;;
  -h | --help)
    echo ""
    echo "load.sh [-p|--port 3868] [-a|--address 127.0.0.1] [-l|--load 1] [-d|--duration 1] [-h|--help]"
    echo "    port     : server's diameter port"
    echo "    address  : server's IP address"
    echo "    load     : calls per second"
    echo "    duration : duration of test"
    echo ""
    exit 0
    ;;
  esac
done

trap ctrl_c INT

function ctrl_c() {
  PID=$(cat /tmp/load.pid)
  printf "Killing load runner %s...\n" "$PID"
  kill -9 "$PID"
  exit 1
}
echo "Starting load $ADDRESS:$PORT with TPS: $LOAD, Duration: $DURATION"
date
nohup java -jar ~/.m2/repository/org/usahin/diameter-test/1.0-SNAPSHOT/diameter-test-1.0-SNAPSHOT.jar "$ADDRESS" "$PORT" "$LOAD" "$DURATION" -Xms256m -Xmx2G > /tmp/load.log 2>&1 &
echo $! > /tmp/load.pid
tail -f /tmp/load.log --pid "$(cat /tmp/load.pid)"
