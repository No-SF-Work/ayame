import os
from compiler import Compiler
from runner import Runner
from analyzer import Analyzer
from pretty_print import Print_C

def get_sy_testcases():
    filelist = []
    for file in os.listdir("testcases"):
        if os.path.splitext(file)[1] == ".sy":
            filelist.append(os.path.splitext(file)[0])
    return sorted(filelist)

testcases = get_sy_testcases()

class Tester:
    def __init__(self, a_scheme):
        self.scheme = a_scheme["scheme"]
        self.frontend_instr = a_scheme["frontend_instr"]
        self.emit_llvm_ir = a_scheme["emit_llvm_ir"]

        self.compiler = Compiler(scheme=self.scheme, testcases=testcases)
        self.runner = Runner(scheme=self.scheme, testcases=testcases)
        self.analyzer = Analyzer(scheme=self.scheme, emit_llvm_ir=self.emit_llvm_ir, testcases=testcases)


    def generate_ir(self):
        for testcase in testcases: self.compiler.sy_to_ir(frontend_instr=self.frontend_instr, testcase=testcase)


    def compile(self):
        self.compiler.compile_all_tests(frontend_instr=self.frontend_instr, emit_llvm_ir=self.emit_llvm_ir)


    def run(self):
        self.runner.run_all_tests()


    def analyze(self):
        self.analyzer.analyze()


    def test(self):
        Print_C.print_header(f"[TESTING {self.scheme}]")
        self.compile()
        self.run()
        self.analyze()
        print()
        print()


    def test_ir(self):
        Print_C.print_header(f"[TESTING {self.scheme}] (IR only)")
        for testcase in testcases:
            Print_C.print_subheader(f"[Compiling {testcase} with {self.scheme}]")
            self.compiler.sy_to_ir(frontend_instr=self.frontend_instr, testcase=testcase)
        print()
        print()