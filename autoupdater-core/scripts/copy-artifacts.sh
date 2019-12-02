#!/usr/bin/env bash

COMPILED_JAR_NAME="autoupdater-core-$1.jar"

#Iterate through arguments except for first.
for i in "${@:2}"
do
    cp $COMPILED_JAR_NAME $i/src/main/resources/$COMPILED_JAR_NAME
done
