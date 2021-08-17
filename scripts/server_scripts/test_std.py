import subprocess
from tester import Tester
from tester import get_sy_testcases
from pretty_print import Print_C

thu_compiler = "thu_compiler "
ustc_compiler = "build/bin/ustc_compiler "
ustc_compiler_no_vec = "build/bin/ustc_compiler_no_vec "
ayame_compiler = "java -classpath src:lib/antlr4-runtime-4.8.jar:lib/argparse4j-0.9.0.jar Compiler "

clang_llvm_scheme = {"scheme": "clang_llvm",
                     "frontend_instr": "clang -x c -c -Ofast -mcpu=cortex-a72 -mfpu=neon -mfloat-abi=hard -S -emit-llvm -include {header} {sy} -o {ir}",
                     "emit_llvm_ir": True}

thu_llvm_scheme = {"scheme": "thu_llvm",
                   "frontend_instr": thu_compiler + "-l {ir} {sy}",
                   "emit_llvm_ir": True}

thu_thu_scheme = {"scheme": "thu_thu",
                  "frontend_instr": thu_compiler + "-o {asm} {sy}",
                  "emit_llvm_ir": False}

ayame_ayame_scheme = {"scheme": "ayame_ayame",
                "frontend_instr": ayame_compiler + "-S {sy} -o {asm} -O2",
                "emit_llvm_ir": False}

# ustc_ustc_scheme = {"scheme": "ustc_ustc",
#                     "frontend_instr": ustc_compiler + "-o {asm} {sy}",
#                     "emit_llvm_ir": False}

# ustc_ustc_no_vec_scheme = {
#                     "scheme": "ustc_ustc_no_vec",
#                     "frontend_instr": ustc_compiler_no_vec + "-o {asm} {sy}",
#                     "emit_llvm_ir": False}

# gcc_gcc_scheme = {"scheme": "gcc_gcc",
#                   "frontend_instr": "gcc -x c -c -Ofast -mcpu=cortex-a72 -mfpu=neon -mfloat-abi=hard -S -include {header} {sy} -o {asm}",
#                   "emit_llvm_ir": False}

# ustc_llvm_scheme = {"scheme": "ustc_llvm", # generate ir only
#                     "frontend_instr": ustc_compiler + "-emit -o {ir} {sy}",
#                     "emit_llvm_ir": True}

# all_schemes = [ayame_ayame_scheme, clang_llvm_scheme, thu_thu_scheme] # gcc_gcc_scheme, ustc_ustc_scheme, ustc_ustc_no_vec_scheme]
all_schemes = [ayame_ayame_scheme]
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
