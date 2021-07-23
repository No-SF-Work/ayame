import subprocess
from tester import Tester
from trivial_presenter import TrivialPresenter
from tester import get_sy_testcases
from pretty_print import Print_C

ayame_compiler = "java -classpath src:lib/antlr4-runtime-4.8.jar:lib/argparse4j-0.9.0.jar Compiler "

ayame_ayame_scheme = {"scheme": "ayame_ayame",
                "frontend_instr": ayame_compiler + "-S {sy} -o {asm}",
                "emit_llvm_ir": False}

ayame_llvm_scheme = {"scheme": "ayame_llvm",
                "frontend_instr": ayame_compiler + "-S {sy} --emit",
                "emit_llvm_ir": True}

Print_C.print_header("[Removing old data...]\n\n")
subprocess.run("rm -rf build/test_results/".split())
subprocess.run("rm -rf build/output/".split())
subprocess.run("rm -rf build/log/compile_log".split())
subprocess.run("rm -rf build/log/run_log".split())
subprocess.run("rm -rf build/log/test_result.log".split())

all_schemes = [ayame_ayame_scheme]#, ayame_llvm_scheme]
testers = []

for scheme in all_schemes:
    tester = Tester(scheme, is_trivial=True)
    testers.append(tester)
    tester.test()

presenter = TrivialPresenter(schemes=[scheme["scheme"] for scheme in all_schemes], testcases=get_sy_testcases())
presenter.present_all_testcases()
