#!/usr/bin/env python3
import dataclasses
import sys
import os
import os.path as path
from dataclasses import dataclass

import matplotlib.pyplot as plt
import datetime
import pandas as pd


def get_start_time(dirname):
    config_file = path.join(dirname, '..', '..', 'config.yaml')
    return path.getmtime(config_file)


def parse_libfuzzer_output(filename, timeout_sec):
    coverage = []
    crashes = []
    features = []
    max_execs = 0

    with open(filename, "r") as file:  # read libfuzzer output
        for line in file:  # and process it line by line
            tokens = line.split()  # split line into tokens

            # A line looks like this:
            #   #33        NEW    cov: 83 ft: 93 corp: 9/48Kb exec/s: 0 rss: 40Mb
            #   L: 1395/30486 MS: 1 InsertRepeatedBytes-
            #
            if len(tokens) < 2: continue

            if tokens[0].startswith('#') and len(tokens) != 4 and len(tokens) != 6:
                print(line)
                e = int(tokens[0][1:])
                if e > max_execs:
                    max_execs = e

            if tokens[0].startswith('#') and len(tokens) >= 14 and (
                tokens[1] == 'NEW' or tokens[1] == 'REDUCE' or tokens[1] == 'pulse'):
                execs = int(tokens[0][1:])
                cov_blks = int(tokens[3])

                coverage.append((execs, cov_blks))

                features.append((execs, int(tokens[5])))

            if tokens[0].startswith("DEDUP_TOKEN"):
                crashes.append(coverage[-1][0])

    _, max_cov = coverage[-1]
    _, max_ft = features[-1]

    coeff = timeout_sec / max_execs
    coverage = list(map(lambda x: (int(x[0] * coeff), x[1]), coverage))
    crashes = list(map(lambda x: (int(x * coeff)), crashes))
    features = list(map(lambda x: (int(x[0] * coeff), x[1]), features))

    coverage = [(0, 0)] + coverage + [(timeout_sec, max_cov)]
    coverage = list(zip(*coverage))

    features = [(0, 0)] + features + [(timeout_sec, max_ft)]
    features = list(zip(*features))

    crashes = [(crashes[i], i + 1) for i in range(len(crashes))]
    crashes = [(0, 0)] + crashes
    crashes = crashes + [(timeout_sec, crashes[-1][1])]
    crashes = list(zip(*crashes))

    return coverage, crashes, features


@dataclass
class LibfuzzerLogEntry:
    execNr: int
    cov: int
    ft: int


# 944	NEW    cov: 57 ft: 58 corp: 10/20b lim: 6 exec/s: 0 rss: 718Mb L: 2/3 MS: 2 ChangeBinInt-Custom-
# 957	REDUCE cov: 63 ft: 64 corp: 11/26b lim: 6 exec/s: 0 rss: 718Mb L: 6/6 MS: 6 CMP-Custom-CrossOver-Custom-ShuffleBytes-Custom- DE: "\377\377\377\000"-

def libfuzzer_output_to_csv(filename) -> str:
    lines = list[LibfuzzerLogEntry]()

    with open(filename, "r") as file:  # read libfuzzer output
        for line in file:  # and process it line by line
            tokens = line.split()  # split line into tokens
            if len(tokens) < 2: continue

            if tokens[0].startswith('#') and len(tokens) >= 14 and (
                tokens[1] == 'NEW' or tokens[1] == 'REDUCE' or tokens[1] == 'pulse'):
                execs = int(tokens[0][1:])
                cov_blks = int(tokens[3])
                cov_ft = int(tokens[5])
                lines.append(LibfuzzerLogEntry(execNr=execs, cov=cov_blks, ft=cov_ft))

            # if tokens[0].startswith("DEDUP_TOKEN"):
            #     crashes.append(coverage[-1][0])
    return pd.DataFrame.from_records(map(lambda x: dataclasses.asdict(x), lines)).to_csv()


def parse_duration(s):
    last = s[-1]
    s = s[:-1]
    if last == 's':
        return int(s)
    elif last == 'm':
        return int(s) * 60
    elif last == 'h':
        return int(s) * 60 * 60


def main():
    if len(sys.argv) != 3:
        print("Usage: python libfuzzer_plot.py <libfuzzer log> <max timeout>")
        print("Example: python3 libfuzzer_plot.py log.txt 2h")
        sys.exit(1)

    jazzer_log = sys.argv[1]
    assert path.exists(jazzer_log)

    coverage, crashes, features = parse_libfuzzer_output(jazzer_log)

    crashes_timestamps, crashes_cnt = crashes
    cov_timestamps, cov = coverage

    ft_timestamps, ft = features

    # plt.plot(crashes_timestamps, crashes_cnt, label="Jazzer Crashes")
    # plt.xlabel("Time (seconds)")
    # plt.ylabel("Crashes")
    # plt.legend()
    # plt.show()
    #
    # plt.plot(cov_timestamps, cov, label="Coverage")
    # plt.xlabel("Time (seconds)")
    # plt.ylabel("Coverage")
    # plt.legend()
    # plt.show()

    plt.plot(ft_timestamps, ft, label="Features")
    plt.xlabel("Time (seconds)")
    plt.ylabel("Features")
    plt.legend()
    plt.show()

    pass


if __name__ == '__main__':
    main()
