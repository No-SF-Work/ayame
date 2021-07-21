#!/bin/bash
cd ..

echo -e "\033[32m\033[1m[Compiling ayame...]\033[0m"
javac -encoding UTF-8 $(find . -name "*.java") -cp lib/antlr4-runtime-4.8.jar:lib/argparse4j-0.9.0.jar
echo -e "\033[32m\033[1m[Compiler compiled]\033[0m"

echo ""
echo -e "\033[32m\033[1m[Running]\033[0m"
python3 scripts/trivial-autotest-ayame.py