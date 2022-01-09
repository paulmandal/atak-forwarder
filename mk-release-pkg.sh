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

repoDirName=`pwd | sed s#.*/##`

pushd ..
if [ "${2}" != "" ]
then
  zip -r /mnt/shared/atak-forwarder-${1}-atak-${2}.zip "${repoDirName}" --exclude "*/build/*" "${repoDirName}/images/*" "${repoDirName}/.git/*" "${repoDirName}/local.properties" "${repoDirName}/.idea/*"
else
  zip -r /mnt/shared/atak-forwarder-${1}.zip "${repoDirName}" --exclude "*/build/*" "${repoDirName}/images/*" "${repoDirName}/.git/*" "${repoDirName}/local.properties" "${repoDirName}/.idea/*"
fi
popd

if [ "${2}" != "" ]
then
  sed -i "s/ext.ATAK_VERSION = \"${2}\"/ext.ATAK_VERSION = \"${currentAtakVersion}\"/" app/build.gradle
  echo "wrote: /mnt/shared/atak-forwarder-${1}-atak-${2}.zip"
else
  echo "wrote: /mnt/shared/atak-forwarder-${1}.zip"
fi

