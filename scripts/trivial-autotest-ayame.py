import subprocess
from tester import Tester
from trivial_presenter import TrivialPresenter
from tester import get_sy_testcases
from pretty_print import Print_C

ayame_compiler = "java -classpath src:lib/antlr4-runtime-4.8.jar:lib/argparse4j-0.9.0.jar Compiler "

ayame_scheme = {"scheme": "ayame_ayame", # generate ir only
                "frontend_instr": ayame_compiler + "-S {sy} -o {asm}",
                "emit_llvm_ir": False}

Print_C.print_header("[Removing old data...]\n\n")
#subprocess.run("rm -rf build/test_results/".split())
subprocess.run("rm -rf build/output/".split())
subprocess.run("rm -rf build/log/compile_log".split())
subprocess.run("rm -rf build/log/run_log".split())
subprocess.run("rm -rf build/log/test_result.log".split())

tester = Tester(ayame_scheme, is_trivial=True)
tester.test()

presenter = TrivialPresenter(schemes=["ayame_ayame"], testcases=get_sy_testcases)
presenter.present_all_testcases()
