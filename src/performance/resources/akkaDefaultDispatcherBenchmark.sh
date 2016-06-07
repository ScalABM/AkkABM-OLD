#!/usr/bin/env bash

BASE_PATH="akka.actor.default-dispatcher"

EXECUTOR="fork-join-executor"

sbt -D$BASE_PATH.$EXECUTOR.parallelism-min=$PARALLELISM_MIN \
    -D$BASE_PATH

'performance:runMain
markets
.AkkaDefaultDispatcherBenchmarkSimulation fork-join-executor 4 64 1000 0.5'