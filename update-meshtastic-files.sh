#!/bin/bash -xe
for filename in DataPacket.kt MyNodeInfo.kt NodeInfo.kt
do
  cp -v ../Meshtastic-Android/app/src/main/java/com/geeksville/mesh/${filename} app/src/main/java/com/geeksville/mesh/${filename}
done

sed -i "s/import com\.geeksville\.mesh\.util\.readParcelableCompat//" app/src/main/java/com/geeksville/mesh/DataPacket.kt
sed -i "s/\.readParcelableCompat/.readParcelable/g" app/src/main/java/com/geeksville/mesh/DataPacket.kt
sed -i "s/import com\.geeksville\.mesh\.util\.anonymize//" app/src/main/java/com/geeksville/mesh/NodeInfo.kt
sed -i "s/\.anonymize//g" app/src/main/java/com/geeksville/mesh/NodeInfo.kt
sed -i "s/import androidx\.room\.Entity//" app/src/main/java/com/geeksville/mesh/MyNodeInfo.kt
sed -i "s/import androidx\.room\.PrimaryKey//" app/src/main/java/com/geeksville/mesh/MyNodeInfo.kt
sed -i "s/@Entity(tableName = \"MyNodeInfo\")//" app/src/main/java/com/geeksville/mesh/MyNodeInfo.kt
sed -i "s/    @PrimaryKey(autoGenerate = false)//" app/src/main/java/com/geeksville/mesh/MyNodeInfo.kt


cp -v ../Meshtastic-Android/app/src/main/aidl/com/geeksville/mesh/IMeshService.aidl app/src/main/aidl/com/geeksville/mesh/IMeshService.aidl
