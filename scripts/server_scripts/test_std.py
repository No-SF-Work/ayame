import subprocess
from tester import Tester
from tester import get_sy_testcases
from pretty_print import Print_C

thu_compiler = "thu_compiler "
ustc_compiler = "build/bin/ustc_compiler "
ustc_compiler_no_vec = "build/bin/ustc_compiler_no_vec "
ayame_compiler = "java -classpath src:lib/antlr4-runtime-4.8.jar:lib/argparse4j-0.9.0.jar Compiler "

clang_llvm_o2_scheme = {"scheme": "clang_llvm_o2",
                        "frontend_instr": "clang -x c -c {sy} {header} -O2 -target armv7-linux-eabi -mcpu=cortex-a72 -mfloat-abi=hard -mfpu=neon -S -o {asm}",
                        "emit_llvm_ir": False}

clang_llvm_o3_scheme = {"scheme": "clang_llvm_o3",
                        "frontend_instr": "clang -x c -c {sy} {header} -O3 -target armv7-linux-eabi -mcpu=cortex-a72 -mfloat-abi=hard -mfpu=neon -S -o {asm}",
                     "emit_llvm_ir": False}

ayame_ayame_scheme = {"scheme": "ayame_ayame",
                "frontend_instr": ayame_compiler + "-S {sy} -o {asm} -O2",
                "emit_llvm_ir": False}

gcc_gcc_o2_scheme = {"scheme": "gcc_gcc_o2",
                     "frontend_instr": "arm-none-linux-gnueabihf-gcc -x c -c -O2 -mcpu=cortex-a72 -mfpu=neon -mfloat-abi=hard -S -include {header} {sy} -o {asm}",
                     "emit_llvm_ir": False}

gcc_gcc_o3_scheme = {"scheme": "gcc_gcc_o3",
                  "frontend_instr": "arm-none-linux-gnueabihf-gcc -x c -c -O3 -mcpu=cortex-a72 -mfpu=neon -mfloat-abi=hard -S -include {header} {sy} -o {asm}",
                  "emit_llvm_ir": False}

# ustc_llvm_scheme = {"scheme": "ustc_llvm", # generate ir only
#                     "frontend_instr": ustc_compiler + "-emit -o {ir} {sy}",
#                     "emit_llvm_ir": True}

# all_schemes = [ayame_ayame_scheme, clang_llvm_scheme, thu_thu_scheme] # gcc_gcc_scheme, ustc_ustc_scheme, ustc_ustc_no_vec_scheme]
all_schemes = [clang_llvm_o2_scheme, clang_llvm_o3_scheme, gcc_gcc_o2_scheme, gcc_gcc_o3_scheme, ayame_ayame_scheme]
testers = []

Print_C.print_header("[Removing old data...]\n\n")
subprocess.run("rm -rf build/test_results/".split())
subprocess.run("rm -rf build/output/".split())
subprocess.run("rm -rf build/log/compile_log".split())
subprocess.run("rm -rf build/log/run_log".split())
subprocess.run("rm -rf build/log/test_result.log".split())

for scheme in all_schemes:
    tester = Tester(scheme, is_trivial=True)
    testers.append(tester)
    tester.test()

# Tester(ustc_llvm_scheme).test_ir()

# presenter = Presenter(schemes=[scheme["scheme"] for scheme in all_schemes], testcases=get_sy_testcases())
# presenter.present_all_testcases()
