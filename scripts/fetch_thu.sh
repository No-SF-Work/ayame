#!/bin/bash
log_file="fetch_thu.log"

mkdir -p build/log
rm -rf build/log/$log_file
touch build/log/$log_file

mkdir -p build/std && cd build/std

echo -e "\033[32m\033[1m[Cloning TrivialCompiler...]\033[0m"

while :
do
	rm -rf TrivialCompiler
	git clone https://github.com/TrivialCompiler/TrivialCompiler.git --depth=1 &>> ../log/$log_file && break

	echo -e "\033[31m\033[1m[Clone Failed]\033[0m"
	echo -e "\033[32m\033[1m[Retry...]\033[0m"
done

echo -e "\033[32m\033[1m[Cloned]\033[0m"

cd TrivialCompiler
echo -e "\033[32m\033[1m[Building]\033[0m"
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release &>> ../../../log/$log_file
make -j 4 &>> ../../../log/$log_file
echo -e "\033[32m\033[1m[Done]\033[0m"

cd ../../.. # back to build/
mkdir -p bin
rm -rf bin/thu_compiler
cp std/TrivialCompiler/build/TrivialCompiler bin/thu_compiler
