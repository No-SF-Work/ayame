#!/bin/bash
log_file="fetch_sysyruntimelibrary.log"

mkdir -p build/log
rm -rf build/log/$log_file
touch build/log/$log_file

rm -rf build/include
rm -rf build/lib
cp -r scripts/ci/include build/include
cp -r scripts/ci/lib build/lib

exit 0

cd build
git clone https://gitlab.eduxiji.net/windcome/sysyruntimelibrary.git &>> log/$log_file

mkdir -p lib
rm -rf libsysy.a
cp sysyruntimelibrary/libsysy.a lib/libsysy.a
