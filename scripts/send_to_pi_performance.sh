#!/bin/bash

echo -e "\033[32m\033[1m[Compressing artifacts...]\033[0m"
tar cf test_results.tar build/test_results
echo -e "\033[32m\033[1m[Compressed.]\033[0m"

echo -e "\033[32m\033[1m[Sending artifacts...]\033[0m"
scp test_results.tar pi@192.168.0.3:~/ci_performance/test_results.tar
echo -e "\033[32m\033[1m[Sent.]\033[0m"

echo -e "\033[32m\033[1m[Uncompressing on PI...]\033[0m"
ssh pi@192.168.0.3 "cd ci_performance; rm -rf build; mkdir -p build/log; tar xf ~/ci_performance/test_results.tar"

echo -e "\033[32m\033[1m[Running on PI...]\033[0m"
ssh pi@192.168.0.3 "cd ci_performance; python3 pi_scripts/test_std.py"