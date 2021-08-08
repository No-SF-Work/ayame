#!/bin/bash

echo -e "\033[32m\033[1m[Compressing artifacts...]\033[0m"
tar cf test_results.tar build/test_results
echo -e "\033[32m\033[1m[Compressed.]\033[0m"

echo -e "\033[32m\033[1m[Sending artifacts...]\033[0m"
scp test_results.tar pi@10.136.89.11:~/ci_functional/test_results.tar
echo -e "\033[32m\033[1m[Sent.]\033[0m"

echo -e "\033[32m\033[1m[Uncompressing on PI...]\033[0m"
ssh pi@10.136.89.11 "cd ci_functional; rm -rf build; mkdir -p build/log; tar xf ~/ci_functional/test_results.tar"

echo -e "\033[32m\033[1m[Running on PI...]\033[0m"
ssh pi@10.136.89.11 "cd ci_functional; python3 pi_scripts/test_std.py"