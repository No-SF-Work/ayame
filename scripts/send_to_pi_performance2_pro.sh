#!/bin/bash

echo -e "\033[32m\033[1m[Compressing artifacts...]\033[0m"
tar cf test_results.tar build/test_results
echo -e "\033[32m\033[1m[Compressed.]\033[0m"

echo -e "\033[32m\033[1m[Sending artifacts...]\033[0m"
scp test_results.tar pi@10.135.99.171:~/ci_performance_pro/test_results.tar
echo -e "\033[32m\033[1m[Sent.]\033[0m"

echo -e "\033[32m\033[1m[Uncompressing on PI...]\033[0m"
ssh pi@10.135.99.171 "cd ci_performance_pro; rm -rf build; mkdir -p build/log; tar xf ~/ci_performance_pro/test_results.tar"

echo -e "\033[32m\033[1m[Running on PI...]\033[0m"
ssh pi@10.135.99.171 "cd ci_performance_pro; python3 pi_scripts/test_std.py"