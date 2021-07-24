import os
import subprocess
from pretty_print import Print_C

class Analyzer:
    def __init__(self, scheme, emit_llvm_ir, testcases) -> None:
        self.scheme = scheme
        self.emit_llvm_ir = emit_llvm_ir
        self.testcases = testcases

        self.asm_template = f"build/test_results/{{testcase}}/asm/{scheme}.s"
        self.perf_template = f"build/test_results/{{testcase}}/analyze/perf/{scheme}.txt"
        self.llvm_mca_template = f"build/test_results/{{testcase}}/analyze/llvm_mca/{scheme}.txt"

        for testcase in testcases:
            self.__generate_path(testcase)

    def __generate_path(self, testcase):
        llvm_mca_path = f"build/test_results/{testcase}/analyze/llvm_mca/"
        perf_path = f"build/test_results/{testcase}/analyze/perf/"

        if not os.path.exists(llvm_mca_path):
            os.makedirs(llvm_mca_path)

        if not os.path.exists(perf_path):
            os.makedirs(perf_path)

    def run_perf(self): # TODO
        pass

    def run_llvm_mca(self, testcase):
        asm = self.asm_template.format(testcase=testcase)
        llvm_mca = self.llvm_mca_template.format(testcase=testcase)

        llvm_mca_file = open(llvm_mca, "w+")

        subprocess.run(f"llvm-mca -mtriple=armv7 -mcpu=cortex-a72 -timeline {asm}".split(), stdout=llvm_mca_file, bufsize=1)

        llvm_mca_file.close()


    def analyze(self):
        for testcase in self.testcases:
            Print_C.print_subheader(f"[Analyzing {self.scheme} | {testcase}]")
            # self.run_llvm_mca(testcase=testcase)

            # self.run_perf()