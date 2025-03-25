#!/usr/bin/env bash

export KFUZZ_JACOCO=/Users/ilma4/Downloads/jacoco-0.8.12/lib/jacocoagent.jar
./kotlinx.fuzz.examples/scripts/run-experiment \
    ./edir \
    . \
   ./kotlinx.fuzz.examples/kotlinx.datetime/targets.txt \
   2h \
    12 \
     kotlinx.fuzz.examples:kotlinx.datetime \
     --classfiles \
     /Users/ilma4/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-datetime-jvm/0.6.1/23d9f71268d3ffc31ce5a5d25622f20bffb3d639/kotlinx-datetime-jvm-0.6.1.jar