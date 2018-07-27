#!/usr/bin/env bash

echo $(pwd)
PROJECT_DIRECTORY=$(pwd)
cd build
echo $(pwd)
echo $PROJECT_DIRECTORY

echo "Running proguard..."
java -jar $PROJECT_DIRECTORY/build/proguard.jar @$PROJECT_DIRECTORY/build/autoupdater-obfuscation.pro

echo "Renaming [$PROJECT_DIRECTORY/AutoUpdaterAPI.jar] to [$PROJECT_DIRECTORY/AutoUpdaterAPI-clean.jar]"
mv $PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar $PROJECT_DIRECTORY/target/AutoUpdaterAPI-clean.jar
echo "Renaming [$PROJECT_DIRECTORY/AutoUpdaterAPI-obfsc.jar] to [$PROJECT_DIRECTORY/AutoUpdaterAPI.jar]"
mv $PROJECT_DIRECTORY/target/AutoUpdaterAPI-obfsc.jar $PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar
