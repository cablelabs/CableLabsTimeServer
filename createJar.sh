#!/bin/sh

rm -rf dist
mkdir dist

jar cfm dist/CableLabsTimeServer.jar src/manifest_server.txt -C bin com
jar cfm dist/CableLabsTimeClient.jar src/manifest_client.txt -C bin com

mkdir dist/lib
cp -r lib/* dist/lib/
