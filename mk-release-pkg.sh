#!/bin/bash
pluginVersion=`grep "ext.PLUGIN_VERSION = \".*\"" app/build.gradle | sed "s/.*ext.PLUGIN_VERSION = \"//" | sed "s/\"//"`
targetAtakVersion="${1}"

if [ "${targetAtakVersion}" != "" ]
then
  currentAtakVersion=`grep "ext.ATAK_VERSION = \".*\"" app/build.gradle | sed "s/.*ext.ATAK_VERSION = \"//" | sed "s/\"//"`
  sed -i "s/ext.ATAK_VERSION = \".*\"/ext.ATAK_VERSION = \"${targetAtakVersion}\"/" app/build.gradle
fi

repoDirName=`pwd | sed s#.*/##`

pushd ..
if [ "${targetAtakVersion}" != "" ]
then
  targetFilename=/mnt/shared/release-pkgs/atak-forwarder-${pluginVersion}-atak-${targetAtakVersion}.zip
else
  targetFilename=/mnt/shared/release-pkgs/atak-forwarder-${pluginVersion}.zip
fi
zip -qr ${targetFilename} "${repoDirName}" --exclude "*/build/*" "${repoDirName}/images/*" "${repoDirName}/local.properties" "${repoDirName}/.idea/*"
popd

if [ "${targetAtakVersion}" != "" ]
then
  sed -i "s/ext.ATAK_VERSION = \"${targetAtakVersion}\"/ext.ATAK_VERSION = \"${currentAtakVersion}\"/" app/build.gradle
  echo "wrote: /mnt/shared/release-pkgs/atak-forwarder-${pluginVersion}-atak-${targetAtakVersion}.zip"
else
  echo "wrote: /mnt/shared/release-pkgs/atak-forwarder-${pluginVersion}.zip"
fi

