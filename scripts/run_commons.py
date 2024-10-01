import logging
import os
import subprocess
from os import path


def get_run_command(target_name):
    command = ["./gradlew", ":test", "--tests", target_name]
    return command


def run_target(target_name, home_dir, logs_dir, jacoco_exec_dir):
    subprocess.run("pwd")
    logging.debug("Running target" + target_name)

    command = get_run_command(target_name)
    logging.debug("command: " + str(command))
    my_env = os.environ.copy()
    my_env["JAZZER_FUZZ"] = "1"
    my_env["JAZZER_COVERAGE_DUMP"] = str(path.join(jacoco_exec_dir, target_name + ".exec"))

    # timestamp = datetime.datetime.now().strftime("%Y-%m-%d--%H-%M-%S")
    stdout_file = open(path.join(logs_dir, target_name), "w")
    stderr_file = open(path.join(logs_dir, target_name) + ".err", "w")
    subprocess.run(command, env=my_env, stderr=stderr_file, stdout=stdout_file, cwd=home_dir)
    stdout_file.close()
    stderr_file.close()
