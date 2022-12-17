#!/bin/bash -xe
for filename in DataPacket.kt MyNodeInfo.kt NodeInfo.kt
do
  cp -v ../Meshtastic-Android/app/src/main/java/com/geeksville/mesh/${filename} app/src/main/java/com/geeksville/mesh/${filename}
done

sed -i "s/import com\.geeksville\.mesh\.util\.anonymize//" app/src/main/java/com/geeksville/mesh/NodeInfo.kt
sed -i "s/\.anonymize//g" app/src/main/java/com/geeksville/mesh/NodeInfo.kt

cp -v ../Meshtastic-Android/app/src/main/aidl/com/geeksville/mesh/IMeshService.aidl app/src/main/aidl/com/geeksville/mesh/IMeshService.aidl
