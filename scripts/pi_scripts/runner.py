import os
import subprocess
from pretty_print import Print_C

class Runner:
    run_kases = 3

    def __init__(self, scheme, testcases):
        self.scheme = scheme
        self.testcases = testcases

        self.bin_file_template = f"build/test_results/{{testcase}}/bin/{scheme}"
        self.myout_template = f"build/output/{{testcase}}/{scheme}.out"
        self.runner_log = f"build/log/run_log/{{testcase}}/{scheme}_{{kase}}.out"

        for testcase in testcases:
            self.__generate_path(testcase)


    def __generate_path(self, testcase):
        myout_path = f"build/output/{testcase}/"
        runner_log_path = f"build/log/run_log/{testcase}/"

        if not os.path.exists(myout_path):
            os.makedirs(myout_path)

        if not os.path.exists(runner_log_path):
            os.makedirs(runner_log_path)


    def run_single_test(self, testcase, kase):
        bin = self.bin_file_template.format(testcase=testcase)
        stdin = f"testcases/{testcase}.in"
        myout = self.myout_template.format(testcase=testcase)
        log = self.runner_log.format(testcase=testcase, kase=kase)

        myout_file = open(myout, "a+")
        log_file = open(log, "a+")
        null_file = open(os.devnull, "w")

        Print_C.print_procedure(f"Running {self.scheme}_{testcase} [kase: {kase}]")

        if os.path.exists(stdin):
            stdin_file = open(stdin, "r")
            if kase == 0:
                p = subprocess.run(f"{bin}".split(), stdin=stdin_file, stdout=myout_file, stderr=log_file, bufsize=1)
                subprocess.run(f"echo".split(), stdout=myout_file, bufsize=1)
                subprocess.run(f"echo {p.returncode}".split(), stdout=myout_file, bufsize=1)
            else:
                p = subprocess.run(f"{bin}".split(), stdin=stdin_file, stdout=null_file, stderr=log_file, bufsize=1)
            stdin_file.close()
        else:
            if kase == 0:
                p = subprocess.run(f"{bin}".split(), stdout=myout_file, stderr=log_file, bufsize=1)
                subprocess.run(f"echo".split(), stdout=myout_file, bufsize=1)
                subprocess.run(f"echo {p.returncode}".split(), stdout=myout_file, bufsize=1)
            else:
                p = subprocess.run(f"{bin}".split(), stdout=null_file, stderr=log_file, bufsize=1)

        myout_file.close()
        log_file.close()


    def run_all_tests(self):
        for kase in range(Runner.run_kases):
            Print_C.print_subheader(f"[Running KASE {kase}]")
            for testcase in self.testcases:
                self.run_single_test(testcase=testcase, kase=kase)

