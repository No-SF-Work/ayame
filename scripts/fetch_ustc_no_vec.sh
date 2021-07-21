#!/bin/bash
log_file="fetch_ustc_no_vec.log"

mkdir -p build/log
rm -rf build/log/$log_file
touch build/log/$log_file

mkdir -p build/std && cd build/std

echo -e "\033[32m\033[1m[Cloning ustc_no_vec...]\033[0m"
while :
do
        rm -rf ustc_no_vec
	git clone https://gitee.com/roife/ustc_no_vec.git --depth=1 &>> ../log/$log_file && break

        echo -e "\033[31m\033[1m[Clone Failed]\033[0m"
        echo -e "\033[32m\033[1m[Retry...]\033[0m"
done
echo -e "\033[32m\033[1m[Cloned]\033[0m"

cd ustc_no_vec
mkdir -p build && cd build

echo -e "\033[32m\033[1m[Building]\033[0m"
cmake .. &>> ../../../log/$log_file
make -j 4 &>> ../../../log/$log_file
echo -e "\033[32m\033[1m[Done]\033[0m"

cd ../../.. # back to build/
mkdir -p bin
rm -rf bin/ustc_compiler_no_vec
cp std/ustc_no_vec/build/compiler bin/ustc_compiler_no_vec
