#!/bin/bash

# get the dependencies
mkdir deps 2>/dev/null || true
cd deps;
if [ ! -f slf4j-simple-1.6.1.jar ]; then
    wget http://www.java2s.com/Code/JarDownload/slf4j/slf4j-simple-1.6.1.jar.zip
    unzip -n slf4j-simple-1.6.1.jar.zip
fi

cd ..

# compile and build the jar
mkdir classes 2>/dev/null || true
javac -cp ../jar/ImpalaService.jar:../deps/libthrift-0.9.1.jar -d classes src/org/ImpalaConnectTest.java
mkdir jar 2>/dev/null || true
jar -cvfm ./jar/ImpalaConnectTest.jar manifest.txt  -C classes .

echo "Build done"