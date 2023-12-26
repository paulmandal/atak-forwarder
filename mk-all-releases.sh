#!/bin/bash
for version in 4.8.1 4.9.0 4.10.0 5.0.0
do
  ./mk-release-pkg.sh $version
done
