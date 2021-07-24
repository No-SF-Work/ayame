#!/bin/bash

echo -e "\033[32m\033[1m[Compressing artifacts...]\033[0m"
zip -q -r test_results.zip build/test_results
echo -e "\033[32m\033[1m[Compressed.]\033[0m"

echo -e "\033[32m\033[1m[Sending artifacts...]\033[0m"
scp test_results.zip pi@10.136.195.142:~/ci/test_results.zip
echo -e "\033[32m\033[1m[Sent.]\033[0m"

echo -e "\033[32m\033[1m[Running on PI...]\033[0m"
ssh pi@10.136.195.142 "unzip -qq -o ~/ci/test_results.zip -d ~/ci"
