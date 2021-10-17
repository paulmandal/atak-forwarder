#!/bin/bash
if [ "${1}" == "" ]
then
  echo "Usage: ${0} <version>"
  exit 1
fi

mv -v local.properties ..
zip -r /mnt/shared/atak-forwarder-${version}-release.zip . -x build app/build images
cp -v ../local.properties .

