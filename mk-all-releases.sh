#!/bin/bash
for version in 4.2.0 4.2.1 4.3.0 4.3.1 4.4.0 4.5.0 4.5.1 4.6.0 4.6.1
do
  ./mk-release-pkg.sh $version
done
