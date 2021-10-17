#!/bin/bash
if [ "${1}" == "" ]
then
  echo "Usage: ${0} <version> [target ATAK version]"
  exit 1
fi

if [ "${2}" != "" ]
then
  currentAtakVersion=`grep ext.ATAK_VERSION = \".*\" | sed "s/.*ext.ATAK_VERSION = \"//" | sed "s/\"//"`
  echo "current atak version: $currentAtakVersion"
  sed -i "s/ext.ATAK_VERSION = \".*\"/ext.ATAK_VERSION = \"${2}\"/" app/build.gradle
fi

mv -v local.properties ..
if [ "${2}" != "" ]
then
  zip -r /mnt/shared/atak-forwarder-${1}-atak-${2}.zip . --exclude build app/build images
else
  zip -r /mnt/shared/atak-forwarder-${1}.zip . --exclude build app/build images
fi

cp -v ../local.properties .

if [ "${2}" != "" ]
then
  sed -i "s/ext.ATAK_VERSION = \"${2}\"/ext.ATAK_VERSION = \"${currentAtakVersion}\"/" app/build.gradle
fi

