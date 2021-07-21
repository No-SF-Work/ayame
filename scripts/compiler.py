import os
import subprocess
from pretty_print import Print_C

lib = "build/lib/libsysy.a"
header = "build/include/sylib.h"

class Compiler:
    def __init__(self, scheme, testcases):
        self.scheme = scheme
        self.testcases = testcases

        self.sy_template = f"testcases/{{testcase}}.sy"
        self.sy_fix_template = f"testcases/{{testcase}}.{scheme}_sy"
        self.llvm_ir_template = f"build/test_results/{{testcase}}/llvm_ir/{scheme}.ll"
        self.asm_template = f"build/test_results/{{testcase}}/asm/{scheme}.s"
        self.obj_template = f"build/test_results/{{testcase}}/obj/{scheme}.o"
        self.bin_template = f"build/test_results/{{testcase}}/bin/{scheme}"
        self.compile_log = f"build/log/compile_log/{{testcase}}/{scheme}.log"

        for testcase in testcases:
            self.__generate_path(testcase)


    def __generate_path(self, testcase):
        llvm_ir_path = f"build/test_results/{testcase}/llvm_ir/"
        asm_path = f"build/test_results/{testcase}/asm/"
        obj_path = f"build/test_results/{testcase}/obj/"
        bin_path = f"build/test_results/{testcase}/bin/"
        compile_log_path = f"build/log/compile_log/{testcase}/"

        if not os.path.exists(llvm_ir_path):
            os.makedirs(llvm_ir_path)

        if not os.path.exists(asm_path):
            os.makedirs(asm_path)

        if not os.path.exists(obj_path):
            os.makedirs(obj_path)

        if not os.path.exists(bin_path):
            os.makedirs(bin_path)

        if not os.path.exists(compile_log_path):
            os.makedirs(compile_log_path)


    def sy_to_ir(self, frontend_instr, testcase):
        sy = self.sy_template.format(testcase=testcase)
        sy_fix = self.sy_fix_template.format(testcase=testcase)
        if os.path.exists(sy_fix):
            sy = sy_fix

        ir = self.llvm_ir_template.format(testcase=testcase)
        log = self.compile_log.format(testcase=testcase)

        log_file = open(log, "a+")

        Print_C.print_procedure(f"Generating {self.scheme}.ll")
        subprocess.run(frontend_instr.format(header=header, sy=sy, ir=ir).split(), stdout=log_file, stderr=log_file, bufsize=1)

        log_file.close()


    def ir_to_asm(self, testcase):
        ir = self.llvm_ir_template.format(testcase=testcase)
        asm = self.asm_template.format(testcase=testcase)
        log = self.compile_log.format(testcase=testcase)

        log_file = open(log, "a+")

        Print_C.print_procedure(f"Generating {self.scheme}.s")
        subprocess.run(f"llc -O3 -march=arm -mcpu=cortex-a72 -float-abi=hard -filetype=asm {ir} -o {asm}".split(), stdout=log_file, stderr=log_file, bufsize=1)

        log_file.close()


    def asm_to_obj(self, testcase):
        asm = self.asm_template.format(testcase=testcase)
        obj = self.obj_template.format(testcase=testcase)
        log = self.compile_log.format(testcase=testcase)

        log_file = open(log, "a+")

        Print_C.print_procedure(f"Generating {self.scheme}.o")
        subprocess.run(f"as -march=armv7-a -mfloat-abi=hard {asm} -o {obj}".split(), stdout=log_file, stderr=log_file, bufsize=1)

        log_file.close()

    def obj_to_bin(self, testcase):
        obj = self.obj_template.format(testcase=testcase)
        bin = self.bin_template.format(testcase=testcase)
        log = self.compile_log.format(testcase=testcase)

        log_file = open(log, "a+")

        Print_C.print_procedure(f"Generating {self.scheme}")
        subprocess.run(f"clang -Ofast -marm -march=armv7-a -mfpu=neon -mfloat-abi=hard {obj} {lib} -o {bin}".split(), stdout=log_file, stderr=log_file, bufsize=1)

        log_file.close()


    def sy_to_asm(self, frontend_instr, testcase):
        asm = self.asm_template.format(testcase=testcase)
        sy = self.sy_template.format(testcase=testcase)
        sy_fix = self.sy_fix_template.format(testcase=testcase)
        if os.path.exists(sy_fix):
            sy = sy_fix

        log = self.compile_log.format(testcase=testcase)

        log_file = open(log, "a+")

        Print_C.print_procedure(f"Generating {self.scheme}.s")
        subprocess.run(frontend_instr.format(header=header, asm=asm, sy=sy).split(), stdout=log_file, stderr=log_file, bufsize=1)


        log_file.close()


    def compile_all_tests(self, frontend_instr, emit_llvm_ir):
        if emit_llvm_ir:
            for testcase in self.testcases:
                Print_C.print_subheader(f"[Compiling {self.scheme} | {testcase}]")
                self.sy_to_ir(frontend_instr=frontend_instr, testcase=testcase)
                self.ir_to_asm(testcase=testcase)
                self.asm_to_obj(testcase=testcase)
                self.obj_to_bin(testcase=testcase)
        else:
            for testcase in self.testcases:
                Print_C.print_subheader(f"[Compiling {self.scheme} | {testcase}]")
                self.sy_to_asm(frontend_instr=frontend_instr, testcase=testcase)
                self.asm_to_obj(testcase=testcase)
                self.obj_to_bin(testcase=testcase)