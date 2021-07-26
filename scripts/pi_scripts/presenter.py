import re
from runner import Runner
from collections import Counter
from pretty_print import Print_C

class Presenter:
    kases = Runner.run_kases
    splitter = re.compile('[-@ :\t\r\n]+')
    scheme_time_print_template = "{0:<10} :  {1:<10.0f}us"
    self.wa_cases = [:]

    def __init__(self, schemes, testcases):
        self.schemes = schemes
        self.testcases = testcases

    def __diff(self, file1, file2):
        file1_lines = list(filter(lambda line: not(line == ""), map(lambda line: line.rstrip('\n'), open(file1).readlines())))
        file2_lines = list(filter(lambda line: not(line == ""), map(lambda line: line.rstrip('\n'), open(file2).readlines())))
        return file1_lines == file2_lines

    def __time_to_us(self, l):
        hours = int(l[0][:-1])
        minutes = int(l[1][:-1])
        seconds = int(l[2][:-1])
        us = int(l[3][:-2])
        return hours * 60 * 60 * 1000000 + minutes * 60 * 1000000 + seconds * 1000000 + us

    def present_single_testcase(self, testcase):
        Print_C.print_subheader(f"[Checking {testcase}]")

        scheme_time_counter = Counter({scheme: 0 for scheme in self.schemes}) # {scheme: time}
        stage_scheme_time_dict = {}
        wrong_schemes = []
        for scheme in self.schemes:
            stdout = f"testcases/{testcase}.out"
            myout = f"build/output/{testcase}/{scheme}.out"
            if not self.__diff(stdout, myout):
                wrong_schemes.append(scheme)
                continue

            stage_time_counter = Counter({}) # {stage: time}
            for kase in range(Presenter.kases):
                runner_log = f"build/log/run_log/{testcase}/{scheme}_{kase}.out"
                lines = open(runner_log).readlines()
                # ['Timer', 'startLine', 'endLine', 'xH', 'xM', 'xS', 'xus'][1:]
                parsed_lines = [Presenter.splitter.split(line)[1:] for line in lines][:-1]
                stage_time = {f"{int(line[0])} ~ {int(line[1])}": self.__time_to_us(line[2:]) for line in parsed_lines}
                stage_time_counter += Counter(stage_time)

            for stage_time in stage_time_counter.items():
                if not stage_time[0] in stage_scheme_time_dict:
                    stage_scheme_time_dict[stage_time[0]] = {}
                stage_scheme_time_dict[stage_time[0]][scheme] = stage_time[1] / Presenter.kases

            scheme_time_counter[scheme] = sum(stage_time_counter.values()) / Presenter.kases

        Print_C.print_procedure("[AVERAGE TIME COUNT (sorted)]")
        sorted_scheme_time_list = sorted(scheme_time_counter.items(), key=lambda scheme_time: scheme_time[1])
        for scheme_time in sorted_scheme_time_list:
            if not scheme_time[0] in wrong_schemes:
                Print_C.print_pass(self.scheme_time_print_template.format(scheme_time[0], scheme_time[1]))
        for scheme in wrong_schemes:
            Print_C.print_error(f"{scheme} WA")

        for stage_scheme_time in stage_scheme_time_dict.items():
            Print_C.print_procedure(f"[Line {stage_scheme_time[0]}]")

            sorted_stage_scheme_list = sorted(stage_scheme_time[1].items(), key=lambda scheme_time: scheme_time[1])
            for scheme_time in sorted_stage_scheme_list:
                print(self.scheme_time_print_template.format(scheme_time[0], scheme_time[1]))

        return scheme_time_counter

    def present_all_testcases(self):
        Print_C.print_header(f"[Checking & Racing]")
        scheme_tot_time_counter = Counter({scheme: 0 for scheme in self.schemes}) # {scheme: total_time}

        for testcase in self.testcases:
            scheme_tot_time_counter += Counter(self.present_single_testcase(testcase=testcase))
            print()

        print()
        sorted_scheme_tot_time_list = sorted(scheme_tot_time_counter.items(), key=lambda scheme_tot_time: scheme_tot_time[1])
        Print_C.print_header(f"[TOTAL TIME RANKING]")
        for scheme_tot_time in sorted_scheme_tot_time_list:
            print(self.scheme_time_print_template.format(scheme_tot_time[0], scheme_tot_time[1]))
        print()
        print()