#!/usr/bin/env bash

PROJECT_DIRECTORY=$(pwd)
cd build

platform='null'
unamestr=`uname`

if [[ "$unamestr" == 'Linux' ]]; then
    echo "Current OS is CentOS."
    platform='centos'
elif [[ "$unamestr" == 'Darwin' ]]; then
    echo "Current OS is MacOS."
    platform='mac'
fi

echo "Working directory is $PROJECT_DIRECTORY"

echo "Running proguard..."
java -jar $PROJECT_DIRECTORY/build/proguard.jar @$PROJECT_DIRECTORY/build/autoupdater-obfuscation-$platform.pro

echo "Renaming [$PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar] to [$PROJECT_DIRECTORY/target/AutoUpdaterAPI-clean.jar]"
mv $PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar $PROJECT_DIRECTORY/target/AutoUpdaterAPI-clean.jar
echo "Renaming [$PROJECT_DIRECTORY/target/AutoUpdaterAPI-obfsc.jar] to [$PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar]"
mv $PROJECT_DIRECTORY/target/AutoUpdaterAPI-obfsc.jar $PROJECT_DIRECTORY/target/AutoUpdaterAPI.jar