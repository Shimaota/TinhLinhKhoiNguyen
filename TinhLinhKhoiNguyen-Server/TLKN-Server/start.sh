#!/bin/sh
# Run from the TLKN-Server folder so server.properties, Assest and ../DataGame resolve correctly.
cd "$(dirname "$0")" || exit 1
if [ ! -f server.properties ]; then
  echo "server.properties not found - copying server.properties.example"
  cp server.properties.example server.properties
fi
if [ ! -f target/tlnt-server.jar ]; then
  ./mvnw -B package || exit 1
fi
exec java -Xms512m -Xmx2g -jar target/tlnt-server.jar
