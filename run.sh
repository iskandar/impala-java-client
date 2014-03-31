#!/bin/bash -ex

LIBS=deps/libthrift-0.9.1.jar:deps/slf4j.api-1.6.1.jar:jar/ImpalaService.jar:test/jar/ImpalaConnectTest.jar

HOST=${1:-localhost}
if [ "$HOST" = "" ]; then
    echo "Please provide a hostname"
    exit 1
fi

QUERY=${2:-"SHOW TABLES"}

java -cp $LIBS \
    org.ImpalaConnectTest.ImpalaConnectTest \
    $HOST \
    "$QUERY"