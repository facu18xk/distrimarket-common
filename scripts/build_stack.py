#!/usr/bin/env python3
"""Build sibling repositories against one freshly installed common artifact."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

WORKSPACE = Path(__file__).resolve().parents[2]
REPOS = ["distrimarket-common", "distrimarket-gateway",
         "distrimarket-ms-featureA", "distrimarket-ms-featureB", "distrimarket-ms-featureC",
         "distrimarket-ms-auth"]
# The gateway repository also contains the Eureka server as a standalone app.
EXTRA_PROJECTS = {"distrimarket-gateway": ["eureka-server"]}


def build_project(directory, label, args, install=False):
    command = [args.maven, "-B"]
    if args.repository:
        command.append("-Dmaven.repo.local=" + str(args.repository.resolve()))
    if args.offline:
        command.append("-o")
    if install:
        command += ["clean", "install"]
    elif args.classes:
        command += ["-X", "clean", "test", "jar:jar"]
    else:
        command += ["clean", "verify"]

    print("Building " + label, flush=True)
    # Common's clean deletes target; record its active log outside the repository.
    logs = WORKSPACE / "distrimarket-common/target/stack-build"
    logs.mkdir(parents=True, exist_ok=True)
    if install:
        descriptor, name = tempfile.mkstemp(prefix="distrimarket-common-build-", suffix=".log")
        os.close(descriptor)
        log = Path(name)
    else:
        log = logs / (label + ".log")

    with log.open("w") as output:
        result = subprocess.run(command, cwd=directory, stdout=output, stderr=subprocess.STDOUT)

    if install:
        logs.mkdir(parents=True, exist_ok=True)
        log = Path(shutil.move(str(log), str(logs / (label + ".log"))))
    if result.returncode:
        print(log.read_text()[-6000:])
        raise SystemExit("Build failed; log: " + str(log))
    if not install and args.classes:
        matches = re.findall(r"\(f\) testPath = \[(.*?)\]", log.read_text())
        if not matches:
            raise SystemExit("Maven did not report the resolved classpath: " + str(log))
        entries = [x for x in matches[-1].split(", ") if not x.endswith("/test-classes")]
        directory.joinpath("target/runtime-classpath.txt").write_text(os.pathsep.join(entries))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven", default=os.environ.get("MAVEN", "mvn"))
    parser.add_argument("--repository", type=Path, help="Isolated Maven repository; seed dependencies from ~/.m2 first")
    parser.add_argument("--classes", action="store_true", help="Compile/test and save classpaths without executable JAR packaging")
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    if not shutil.which(args.maven):
        parser.error("Maven not found: set MAVEN or pass --maven")

    for repo in REPOS:
        build_project(WORKSPACE / repo, repo, args, install=(repo == "distrimarket-common"))
        for subdir in EXTRA_PROJECTS.get(repo, []):
            build_project(WORKSPACE / repo / subdir, repo + "-" + subdir, args)

    print("All six repositories (seven applications) built successfully.", flush=True)


if __name__ == "__main__":
    main()
