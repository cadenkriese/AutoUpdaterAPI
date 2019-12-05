#!/usr/bin/env bash

COMPILED_JAR_NAME="autoupdater-plugin-$1.jar"

#Iterate through arguments except for first.
for i in "${@:2}"
do
    mkdir -p $i/src/main/resources/ && cp $COMPILED_JAR_NAME $i/src/main/resources/$COMPILED_JAR_NAME
done
